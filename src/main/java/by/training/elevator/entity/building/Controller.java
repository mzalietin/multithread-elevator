package by.training.elevator.entity.building;

import by.training.elevator.conf.Configuration;
import by.training.elevator.conf.MessageConstants;
import by.training.elevator.entity.TransportationTask;
import by.training.elevator.entity.passenger.Passenger;
import by.training.elevator.entity.passenger.TransportationState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Elevator operator implementation for managing multithreaded transportation of passengers.
 */
public class Controller {
    private final Elevator elevator;

    private static final Logger logger = LogManager.getRootLogger();
    private final AtomicInteger ignorePassengersCount = new AtomicInteger();
    private final Set<Integer> emptyDispatches = new HashSet<>();
    private final int levelsCount;

    private final Lock lock = new ReentrantLock(true);
    private final Condition[] elevatorArrived;
    private final Condition elevatorMoved; //todo can be replaced?

    private Level currentLevel;
    private boolean directionUp;

    /**
     * Single constructor which performs vital initials, specifically creates condition object instance for each level
     * and for transportation finish. Checks input argument for negative value.
     *
     * @param levelsCount configuration parameter of levels count.
     */
    public Controller(int levelsCount, Elevator elevator) {
        if (levelsCount > 0) {
            this.elevatorArrived = new Condition[levelsCount];
            for (int i = 0; i < levelsCount; i++) {
                this.elevatorArrived[i] = lock.newCondition();
            }
            this.elevatorMoved = lock.newCondition();
            this.levelsCount = levelsCount;
        } else {
            throw new IllegalArgumentException("Levels count cannot be negative/zero");
        }
        this.elevator = Objects.requireNonNull(elevator);
        this.currentLevel = elevator.getBuilding().getLevels().get(0);
        this.directionUp = true;
    }

    /**
     * Acquires operator's monitor.
     */
    public void acquireLock() {
        lock.lock();
    }

    /**
     * Releases operator's monitor.
     */
    public void releaseLock() {
        lock.unlock();
    }

    /**
     * Simple getter for test purposes.
     *
     * @return level value.
     */
    public int getCurrentLevel() {
        return currentLevel.getValue();
    }

    /**
     * Simple getter for test purposes.
     *
     * @return elevator
     */
    public Elevator getElevator() {
        return elevator;
    }

    /**
     * Checks if remaining passengers count in current level's dispatch container is equal to passengers count, that
     * will not be transported from current level because of direction discrepancy with elevator's direction.
     *
     * @return true if all passengers in dispatch are ignored.
     */
    private boolean remainingPassengersInIgnore() {
        return currentLevel.getDispatchContainer().size() == ignorePassengersCount.get();
    }

    /**
     * Method to launch transportation process. Creates transportation tasks for each passenger, then starts operartor's
     * workflow thread and tasks threadpool.
     */
    public void startTransportation() {
        List<TransportationTask> taskList = new ArrayList<>();
        for (Level st: elevator.getBuilding().getLevels()) {
            for (Passenger pas: st.getDispatchContainer()) {
                taskList.add(new TransportationTask(pas, this));
            }
        }

        try {
            ExecutorService controllerExecutor = Executors.newSingleThreadScheduledExecutor();
            controllerExecutor.submit(new Work());

            ExecutorService threadPool = Executors.newFixedThreadPool(taskList.size());
            threadPool.invokeAll(taskList);

            threadPool.shutdown();
            controllerExecutor.shutdown();
        } catch (InterruptedException e) {
            logger.catching(e);
        }
    }

    /**
     * Validates transportation process finish via java asserts.
     */
    public void validateFinish() {
        logger.info("VALIDATION PROCESS");

        int arrivedPassengersCount = 0;

        for (Level st: elevator.getBuilding().getLevels()) {
            logger.info("Level {} dispatch is empty - {}", st.getValue(), st.getDispatchContainer().isEmpty());
            assert st.getDispatchContainer().isEmpty();

            arrivedPassengersCount += st.getArrivalContainer().size();

            for (Passenger pas: st.getArrivalContainer()) {
                logger.info("Passenger #{} with destination {} is in level {} arrival", pas.getId(), pas.getDestinationLevel(), st.getValue());
                assert pas.getDestinationLevel() == st.getValue();

                logger.info("Passenger #{} state {}", pas.getId(), pas.getState());
                assert pas.getState() == TransportationState.COMPLETED;
            }
        }

        logger.info("Arrived passenger count - {}, initial passenger count - {}", arrivedPassengersCount, Configuration.PASSENGERS_NUMBER);
        assert arrivedPassengersCount == Configuration.PASSENGERS_NUMBER;
    }

    /**
     * Represents controller workflow.
     */
    private class Work extends Thread {

        /**
         * Thread will not stop until count of empty dispatches will not be equal to levels count and elevator
         * container is not emty. Next cycle of elevator move performs when it's container is full or when all
         * passengers left in dispatch are in transportation ignore.
         */
        @Override
        public void run() {
            logger.info(MessageConstants.STARTING_TRANSPORTATION);

            while (emptyDispatches.size() != levelsCount || !elevator.isEmpty()) {
                if (elevator.isFull() || remainingPassengersInIgnore()) {
                    try {
                        lock.lock();

                        moveElevator();

                        if (currentLevel.getDispatchContainer().isEmpty()) {
                            emptyDispatches.add(currentLevel.getValue());
                        }
                        ignorePassengersCount.set(0);

                        notifyNewLevel();
                    }  finally {
                        lock.unlock();
                    }
                }
            }

            logger.info(MessageConstants.TRANSPORTATION_COMPLETE);
        }
    }

    /**
     * Moves elevator to the next level according to its current direction.
     */
    private void moveElevator() {
        int nextLevel;

        if (directionUp) {
            nextLevel = currentLevel.getValue() + 1;
            if (nextLevel == levelsCount - 1) {
                directionUp = false;
            }
        } else {
            nextLevel = currentLevel.getValue() - 1;
            if (nextLevel == 0) {
                directionUp = true;
            }
        }

        logger.info(MessageConstants.MOVING_ELEVATOR, currentLevel.getValue(), nextLevel);
        currentLevel = elevator.getBuilding().getLevels().get(nextLevel);
    }

    /**
     * Notifies passengers that elevator arrived to the new level and people that wait in this level's dispatch to be
     * transported.
     */
    private void notifyNewLevel() {
        elevatorMoved.signalAll();
        elevatorArrived[currentLevel.getValue()].signalAll();
    }

    /**
     * Guarantees that passenger will be accepted to elevators cab if and only if: elevator is arrived to the level in
     * which dispatch he is waiting on; passengers transportation direction is similar to the current direction of elevator;
     * elevator's cabine is not full. Also, if elevator has arrived to passengers initial level, but their directions doesn't
     * match, passenger is added to ignore counter.
     *
     * @param passenger passenger that should await his boarding to elevator.
     */
    public void awaitBoarding(Passenger passenger) {
        Objects.requireNonNull(passenger);
        try {
            while (passenger.getInitialLevel() != currentLevel.getValue()
                    || passenger.isDestinationUpward() != directionUp
                    || elevator.isFull()) {

                if (passenger.getInitialLevel() == currentLevel.getValue() && passenger.isDestinationUpward() != directionUp) {
                    ignorePassengersCount.incrementAndGet();
                }

                elevatorArrived[passenger.getInitialLevel()].await();
            }
        } catch (InterruptedException e) {
            logger.catching(e);
        }
    }

    /**
     * Boards specified passenger to elevator. If current level dispatch is empty, increments corresponding counter.
     *
     * @param passenger passenger to be embarked to elevators cab.
     */
    public void boardPassenger(Passenger passenger) {
        Objects.requireNonNull(passenger);
        logger.info(MessageConstants.BOARDING_OF_PASSENGER, passenger.getId(), currentLevel.getValue());
        currentLevel.getDispatchContainer().remove(passenger);
        elevator.boardPassenger(passenger);
        if (currentLevel.getDispatchContainer().isEmpty()) {
            emptyDispatches.add(currentLevel.getValue());
        }
    }

    /**
     * Orders transportation task to wait until specified passenger isn't transported.
     *
     * @param passenger passenger expecting to be transported.
     */
    public void awaitTransportation(Passenger passenger) {
        Objects.requireNonNull(passenger);
        try {
            while (passenger.getDestinationLevel() != currentLevel.getValue()) {
                elevatorMoved.await();
            }
        } catch (InterruptedException e) {
            logger.catching(e);
        }
    }

    /**
     * Removes passenger from elevator when he is transported to his destination level.
     *
     * @param passenger passenger to deboard from elevator.
     */
    public void unboardPassenger(Passenger passenger) {
        Objects.requireNonNull(passenger);
        logger.info(MessageConstants.UNBOARDING_OF_PASSENGER, passenger.getId(), currentLevel.getValue());
        elevator.unboardPassenger(passenger);
        currentLevel.getArrivalContainer().add(passenger);
    }
}
