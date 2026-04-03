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
    private Level currentLevel;

    private final Logger logger = LogManager.getRootLogger();
    private final AtomicInteger ignorePassengersCount = new AtomicInteger();
    private final Set<Integer> emptyDispatch = new HashSet<>();

    private final Lock lock = new ReentrantLock(true);
    private final Condition[] levelConditions;
    private final Condition transported;

    /**
     * Single constructor which performs vital initials, specifically creates condition object instance for each level
     * and for transportation finish. Checks input argument for negative value.
     *
     * @param levelsCount configuration parameter of levels count.
     */
    public Controller(int levelsCount, Elevator elevator) {
        if (levelsCount > 0) {
            this.levelConditions = new Condition[levelsCount];
            for (int i = 0; i < levelsCount; i++) {
                this.levelConditions[i] = lock.newCondition();
            }
            this.transported = lock.newCondition();
        } else {
            throw new IllegalArgumentException("Stories count cannot be negative/zero");
        }
        this.elevator = Objects.requireNonNull(elevator);
        this.currentLevel = elevator.getBuilding().getLevels().get(0);
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
     * @return level value of current story.
     */
    public int getCurrentLevel() {
        return currentLevel.getValue();
    }

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
                logger.info("Passenger #{} with destination {} is in story {} arrival", pas.getId(), pas.getDestinationLevel(), st.getValue());
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

            while (emptyDispatch.size() != levelConditions.length || !elevator.isEmpty()) {
                if (elevator.isFull() || remainingPassengersInIgnore()) {
                    try {
                        lock.lock();

                        moveElevator();

                        if (currentLevel.getDispatchContainer().isEmpty()) {
                            emptyDispatch.add(currentLevel.getValue());
                        }
                        ignorePassengersCount.set(0);

                        notifyNewStory();
                    }  finally {
                        lock.unlock();
                    }
                }
            }

            logger.info(MessageConstants.TRANSPORTATION_COMPLETE);
        }
    }

    /**
     * Moves elevator to the next story according to it's current direction.
     */
    private void moveElevator() {
        int nextLevel;

        if (elevator.isDirectionUp()) {
            nextLevel = currentLevel.getValue() + 1;
            if (nextLevel == levelConditions.length - 1) {
                elevator.setDirectionUp(false);
            }
        } else {
            nextLevel = currentLevel.getValue() - 1;
            if (nextLevel == 0) {
                elevator.setDirectionUp(true);
            }
        }

        logger.info(MessageConstants.MOVING_ELEVATOR, currentLevel.getValue(), nextLevel);
        currentLevel = elevator.getBuilding().getLevels().get(nextLevel);
    }

    /**
     * Notifies passengers that elevator arrived to the new story and people that wait in this story dispatch to be
     * transported.
     */
    private void notifyNewStory() {
        transported.signalAll();
        levelConditions[currentLevel.getValue()].signalAll();
    }

    /**
     * Guarantees that passenger will be accepted to elevators cab if and only if: elevator is arrived to the story in
     * which dispatch he is waiting on; passengers transportation direction is similar to current elevator's direction;
     * elevators cabine is not full. Also if elevator has arrived to passengers story, but theirs directions doesn't
     * correlate, passenger signs in ignore counter.
     *
     * @param passenger passenger that should await his boarding to elevator.
     */
    public void awaitBoarding(Passenger passenger) {
        Objects.requireNonNull(passenger);
        try {
            while (passenger.getInitialLevel() != currentLevel.getValue()
                    || passenger.isDestinationUpward() != elevator.isDirectionUp()
                    || elevator.isFull()) {

                if (passenger.getInitialLevel() == currentLevel.getValue()
                        && passenger.isDestinationUpward() != elevator.isDirectionUp()) {
                    ignorePassengersCount.incrementAndGet();
                }

                levelConditions[passenger.getInitialLevel()].await();
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
            emptyDispatch.add(currentLevel.getValue());
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
                transported.await();
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
