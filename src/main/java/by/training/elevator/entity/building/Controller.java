package by.training.elevator.entity.building;

import by.training.elevator.conf.Configuration;
import by.training.elevator.conf.MessageConstants;
import by.training.elevator.entity.TransportationTask;
import by.training.elevator.entity.passenger.Passenger;
import by.training.elevator.entity.passenger.TransportationState;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Controller {
    private final Elevator elevator;

    private static final Logger logger = LogManager.getRootLogger();
    private final Set<Level> emptyLevels = Collections.synchronizedSet(new HashSet<>());
    private final int levelsCount;
    private final int minLevel;
    private final int maxLevel;

    private final Lock lock = new ReentrantLock(true);
    private final Condition[] elevatorArrived;
    private final Condition[] onboarding;

    private Level currentLevel;
    private boolean directionUp;

    public Controller(int levelsCount, Elevator elevator) {
        this.elevatorArrived = new Condition[levelsCount];
        this.onboarding = new Condition[levelsCount];
        for (int i = 0; i < levelsCount; i++) {
            this.elevatorArrived[i] = lock.newCondition();
            this.onboarding[i] = lock.newCondition();
        }
        this.levelsCount = levelsCount;
        this.minLevel = 0;
        this.maxLevel = levelsCount - 1;
        this.elevator = Objects.requireNonNull(elevator);
        this.currentLevel = elevator.getBuilding().getLevels().get(minLevel);
        this.directionUp = true;
    }

    public int getCurrentLevel() {
        return currentLevel.getValue();
    }

    public Elevator getElevator() {
        return elevator;
    }

    public void startTransportation() {
        try {
            List<Passenger> passengers = elevator.getBuilding().remainingPassengers();

            ExecutorService threadPool = Executors.newFixedThreadPool(passengers.size());

            passengers.forEach(pas -> threadPool.submit(new TransportationTask(pas, this)));

            Worker controllerWorker = new Worker();

            controllerWorker.start();
            controllerWorker.join();

            threadPool.shutdown();
        } catch (InterruptedException e) {
            logger.catching(e);
        }
    }

    public void awaitBoarding(Passenger passenger) {
        try {
            lock.lock();
            do {
                onboarding[passenger.getInitialLevel()].await();
            } while (!directionsMatch(passenger) || elevator.isFull());
        } catch (InterruptedException e) {
            logger.catching(e);
        }
    }

    private boolean directionsMatch(Passenger passenger) {
        return passenger.isDestinationUpward() == directionUp;
    }

    public void boardPassenger(Passenger passenger) {
        assert passenger.getInitialLevel() == currentLevel.getValue();
        logger.info(MessageConstants.BOARDING_OF_PASSENGER, passenger.getId(), currentLevel.getValue());
        currentLevel.removeFromDeparture(passenger);
        elevator.boardPassenger(passenger);
        passenger.setState(TransportationState.IN_PROGRESS);
        if (currentLevel.departuresEmpty()) {
            emptyLevels.add(currentLevel);
        }
    }

    public void awaitTransportation(Passenger passenger) {
        try {
            elevatorArrived[passenger.getDestinationLevel()].await();
        } catch (InterruptedException e) {
            logger.catching(e);
        }
    }

    public void unboardPassenger(Passenger passenger) {
        assert passenger.getDestinationLevel() == currentLevel.getValue();
        logger.info(MessageConstants.UNBOARDING_OF_PASSENGER, passenger.getId(), currentLevel.getValue());
        elevator.unboardPassenger(passenger);
        currentLevel.addToArrival(passenger);
        lock.unlock();
    }

    private class Worker extends Thread {

        @Override
        public void run() {
            logger.info(MessageConstants.STARTING_TRANSPORTATION);
            // save empty levels dispatches beforehand
            elevator.getBuilding().getLevels().stream()
                    .filter(Level::departuresEmpty)
                    .forEach(emptyLevels::add);
            // initial signal
            try {
                lock.lock();
                onboarding[0].signalAll();
            } finally {
                lock.unlock();
            }
            while (emptyLevels.size() != levelsCount || !elevator.isEmpty()) {
                moveElevator();
            }

            logger.info(MessageConstants.TRANSPORTATION_COMPLETE);
        }
    }

    private void moveElevator() {
        try {
            lock.lock();

            int nextLevel;
            int curLevel = currentLevel.getValue();

            OptionalInt elevatorNextDestination = elevator.closestDestination(directionUp, curLevel);
            boolean elevatorNextDestinationPresent = elevatorNextDestination.isPresent();
            OptionalInt nextAwaitingPassengerLevel;

            if (directionUp) {
                List<Level> upperLevels = elevator.getBuilding().getLevels().subList(curLevel + 1, levelsCount);
                if (elevatorNextDestination.isPresent()) {
                    nextAwaitingPassengerLevel = upperLevels.stream()
                            .filter(l -> !l.departureUpEmpty())
                            .mapToInt(Level::getValue)
                            .min();
                    if (nextAwaitingPassengerLevel.isPresent()) {
                        nextLevel = Integer.min(elevatorNextDestination.getAsInt(), nextAwaitingPassengerLevel.getAsInt());
                    } else {
                        nextLevel = elevatorNextDestination.getAsInt();
                    }
                } else {
                    nextAwaitingPassengerLevel = upperLevels.stream()
                            .filter(l -> !l.departureUpEmpty() || !l.departureDownEmpty())
                            .mapToInt(Level::getValue)
                            .min();
                    if (nextAwaitingPassengerLevel.isPresent()) {
                        nextLevel = nextAwaitingPassengerLevel.getAsInt();
                    } else {
                        logger.warn("directionUp = {}", directionUp);
                        logger.warn("cabin = {}", elevator.getCabin());
                        logger.warn("remainingPassengers = {}", elevator.getBuilding().remainingPassengers());
                        throw new IllegalStateException("Should not get here!");
                    }
                }
            } else {
                List<Level> lowerLevels = elevator.getBuilding().getLevels().subList(minLevel, curLevel);
                if (elevatorNextDestination.isPresent()) {
                    nextAwaitingPassengerLevel = lowerLevels.stream()
                            .filter(l -> !l.departureDownEmpty())
                            .mapToInt(Level::getValue)
                            .max();
                    if (nextAwaitingPassengerLevel.isPresent()) {
                        nextLevel = Integer.max(elevatorNextDestination.getAsInt(), nextAwaitingPassengerLevel.getAsInt());
                    } else {
                        nextLevel = elevatorNextDestination.getAsInt();
                    }
                } else {
                    nextAwaitingPassengerLevel = lowerLevels.stream()
                            .filter(l -> !l.departureUpEmpty() || !l.departureDownEmpty())
                            .mapToInt(Level::getValue)
                            .max();
                    if (nextAwaitingPassengerLevel.isPresent()) {
                        nextLevel = nextAwaitingPassengerLevel.getAsInt();
                    } else {
                        logger.warn("directionUp = {}", directionUp);
                        logger.warn("cabin = {}", elevator.getCabin());
                        logger.warn("remainingPassengers = {}", elevator.getBuilding().remainingPassengers());
                        throw new IllegalStateException("Should not get here!");
                    }
                }
            }

            logger.info(MessageConstants.MOVING_ELEVATOR, directionUp ? "UP" : "DOWN", curLevel, nextLevel);

            currentLevel = elevator.getBuilding().getLevels().get(nextLevel);

            elevatorArrived[currentLevel.getValue()].signalAll();

            if (directionUp) {
                if ((currentLevel.departureUpEmpty() && elevator.isEmpty()) || currentLevel.getValue() == maxLevel) {
                    logger.info("flip direction to DOWN");
                    directionUp = false;
                }
            } else {
                if (currentLevel.departureDownEmpty() && elevator.isEmpty() || currentLevel.getValue() == minLevel) {
                    logger.info("flip direction to UP");
                    directionUp = true;
                }
            }

            onboarding[currentLevel.getValue()].signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void validateFinish() {
        logger.info("------ VALIDATION PROCESS ------");

        int arrivedPassengersCount = 0;

        for (Level st : elevator.getBuilding().getLevels()) {
            logger.info("Level {} dispatch is empty - {}", st.getValue(), st.departuresEmpty());
            assert st.departuresEmpty();

            arrivedPassengersCount += st.arrivedPassengers().size();

            for (Passenger pas : st.arrivedPassengers()) {
                logger.info("Passenger #{} with destination {} is in level {} arrival", pas.getId(), pas.getDestinationLevel(),
                        st.getValue());
                assert pas.getDestinationLevel() == st.getValue();

                logger.info("Passenger #{} state {}", pas.getId(), pas.getState());
                assert pas.getState() == TransportationState.COMPLETED;
            }
        }

        logger.info("Arrived passenger count - {}, initial passenger count - {}", arrivedPassengersCount, Configuration.PASSENGERS_NUMBER);
        assert arrivedPassengersCount == Configuration.PASSENGERS_NUMBER;
    }
}
