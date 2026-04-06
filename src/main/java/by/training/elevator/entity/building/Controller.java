package by.training.elevator.entity.building;

import by.training.elevator.conf.Configuration;
import by.training.elevator.conf.MessageConstants;
import by.training.elevator.entity.TransportationTask;
import by.training.elevator.entity.passenger.Passenger;
import by.training.elevator.entity.passenger.TransportationState;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
    private final Set<Level> emptyLevels = new HashSet<>();
    private final int levelsCount;
    private final int minLevel;
    private final int maxLevel;

    private final Lock lock = new ReentrantLock(true);
    private final Condition[] elevatorArrived;

    private Level currentLevel;
    private boolean directionUp;

    public Controller(int levelsCount, Elevator elevator) {
        if (levelsCount > 0) {
            this.elevatorArrived = new Condition[levelsCount];
            for (int i = 0; i < levelsCount; i++) {
                this.elevatorArrived[i] = lock.newCondition();
            }
            this.levelsCount = levelsCount;
            this.minLevel = 0;
            this.maxLevel = levelsCount - 1;
            this.elevator = Objects.requireNonNull(elevator);
            this.currentLevel = elevator.getBuilding().getLevels().get(0);
            this.directionUp = true;
        } else {
            throw new IllegalArgumentException("Levels count cannot be negative/zero");
        }
    }

    public void acquireLock() {
        lock.lock();
    }

    public void releaseLock() {
        lock.unlock();
    }

    public int getCurrentLevel() {
        return currentLevel.getValue();
    }

    public Elevator getElevator() {
        return elevator;
    }

    public void startTransportation() {
        try {
            List<Passenger> passengers = elevator.getBuilding().allPassengers();

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

    private void moveElevator() {
        int nextLevel;

        if (directionUp) {
            nextLevel = currentLevel.getValue() + 1;
        } else {
            nextLevel = currentLevel.getValue() - 1;
        }

        logger.info(MessageConstants.MOVING_ELEVATOR, directionUp ? "UP" : "DOWN", currentLevel.getValue(), nextLevel);
        currentLevel = elevator.getBuilding().getLevels().get(nextLevel);

        if (currentLevel.getValue() == maxLevel) {
            directionUp = false;
        } else if (currentLevel.getValue() == minLevel) {
            directionUp = true;
        }
    }

    private void notifyNewLevel() {
        elevatorArrived[currentLevel.getValue()].signalAll();
    }

    public void awaitBoarding(Passenger passenger) {
        try {
            do {
                elevatorArrived[passenger.getInitialLevel()].await();
            } while (!directionsMatch(passenger) || elevator.isFull());
        } catch (InterruptedException e) {
            logger.catching(e);
        }
    }

    private boolean directionsMatch(Passenger passenger) {
        return passenger.isDestinationUpward() == directionUp;
    }

    public void boardPassenger(Passenger passenger) {
        logger.info(MessageConstants.BOARDING_OF_PASSENGER, passenger.getId(), currentLevel.getValue());
        currentLevel.removeFromDeparture(passenger);
        elevator.boardPassenger(passenger);
        passenger.setState(TransportationState.IN_PROGRESS);
        if (currentLevel.isDepartureEmpty()) {
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
        logger.info(MessageConstants.UNBOARDING_OF_PASSENGER, passenger.getId(), currentLevel.getValue());
        elevator.unboardPassenger(passenger);
        currentLevel.addToArrival(passenger);
    }

    private class Worker extends Thread {

        @Override
        public void run() {
            logger.info(MessageConstants.STARTING_TRANSPORTATION);
            // save empty levels dispatches beforehand
            elevator.getBuilding().getLevels().stream()
                    .filter(Level::isDepartureEmpty)
                    .forEach(emptyLevels::add);
            // initial signal
            try {
                lock.lock();
                elevatorArrived[0].signalAll();
            } finally {
                lock.unlock();
            }
            while (true) {
                try {
                    lock.lock();
                    if (emptyLevels.size() == levelsCount && elevator.isEmpty()) {
                        break;
                    }
                    moveElevator();
                    notifyNewLevel();
                } finally {
                    lock.unlock();
                }
            }

            logger.info(MessageConstants.TRANSPORTATION_COMPLETE);
        }
    }

    public void validateFinish() {
        logger.info("------ VALIDATION PROCESS ------");

        int arrivedPassengersCount = 0;

        for (Level st : elevator.getBuilding().getLevels()) {
            logger.info("Level {} dispatch is empty - {}", st.getValue(), st.isDepartureEmpty());
            assert st.isDepartureEmpty();

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
