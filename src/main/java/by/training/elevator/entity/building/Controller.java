package by.training.elevator.entity.building;

import by.training.elevator.conf.Configuration;
import by.training.elevator.conf.MessageConstants;
import by.training.elevator.entity.TransportationTask;
import by.training.elevator.entity.passenger.Passenger;
import by.training.elevator.entity.passenger.TransportationState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Elevator operator implementation for managing multithreaded transportation of passengers.
 */
public class Controller {
    private Elevator elevator;
    private Storey currentStorey;

    private final Logger logger = LogManager.getRootLogger();
    private final AtomicInteger ignorePassengersCount = new AtomicInteger();
    private final Set<Integer> emptyDispatch = new HashSet<>();

    private final Lock lock = new ReentrantLock(true);
    private final Condition[] storyConditions;
    private final Condition transported;

    /**
     * Single constructor which performs vital initials, specifically creates condition object instance for each storey
     * and for transportation finish. Checks input argument for negative value.
     *
     * @param storiesCount configuration parameter of storeys count.
     */
    public Controller(int storiesCount) {
        if (storiesCount > 0) {
            storyConditions = new Condition[storiesCount];
            for (int i = 0; i < storiesCount; i++) {
                storyConditions[i] = lock.newCondition();
            }
            transported = lock.newCondition();
        } else {
            throw new IllegalArgumentException("Stories count cannot be negative/zero");
        }
    }

    public void setElevator(Elevator elevator) {
        this.elevator = Objects.requireNonNull(elevator);
        currentStorey = elevator.getBuilding().getStories().get(0);
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
    public int getCurrentStorey() {
        return currentStorey.getLevel();
    }

    /**
     * Checks if remaining passengers count in current storey's dispatch container is equal to passengers count, that
     * will not be transported from current storey because of direction discrepancy with elevator's direction.
     *
     * @return true if all passengers in dispatch are ignored.
     */
    private boolean remainingPassengersInIgnore() {
        return currentStorey.getDispatchContainer().size() == ignorePassengersCount.get();
    }

    /**
     * Method to launch transportation process. Creates transportation tasks for each passenger, then starts operartor's
     * workflow thread and tasks threadpool.
     */
    public void startTransportation() {
        List<TransportationTask> taskList = new ArrayList<>();
        for (Storey st: elevator.getBuilding().getStories()) {
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

        for (Storey st: elevator.getBuilding().getStories()) {
            logger.info("Storey {} dispatch is empty - {}", st.getLevel(), st.getDispatchContainer().isEmpty());
            assert st.getDispatchContainer().isEmpty();

            arrivedPassengersCount += st.getArrivalContainer().size();

            for (Passenger pas: st.getArrivalContainer()) {
                logger.info("Passenger #{} with destination {} is in story {} arrival", pas.getId(), pas.getDestinationStory(), st.getLevel());
                assert pas.getDestinationStory() == st.getLevel();

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
         * Thread will not stop until count of empty dispatches will not be equal to storeys count and elevator
         * container is not emty. Next cycle of elevator move performs when it's container is full or when all
         * passengers left in dispatch are in transportation ignore.
         */
        @Override
        public void run() {
            logger.info(MessageConstants.STARTING_TRANSPORTATION);

            while (emptyDispatch.size() != storyConditions.length || !elevator.isEmpty()) {
                if (elevator.isFull() || remainingPassengersInIgnore()) {
                    try {
                        lock.lock();

                        moveElevator();

                        if (currentStorey.getDispatchContainer().isEmpty()) {
                            emptyDispatch.add(currentStorey.getLevel());
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
            nextLevel = currentStorey.getLevel() + 1;
            if (nextLevel == storyConditions.length - 1) {
                elevator.setDirectionUp(false);
            }
        } else {
            nextLevel = currentStorey.getLevel() - 1;
            if (nextLevel == 0) {
                elevator.setDirectionUp(true);
            }
        }

        logger.info(MessageConstants.MOVING_ELEVATOR, currentStorey.getLevel(), nextLevel);
        currentStorey = elevator.getBuilding().getStories().get(nextLevel);
    }

    /**
     * Notifies passengers that elevator arrived to the new story and people that wait in this story dispatch to be
     * transported.
     */
    private void notifyNewStory() {
        transported.signalAll();
        storyConditions[currentStorey.getLevel()].signalAll();
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
            while (passenger.getInitialStory() != currentStorey.getLevel()
                    || passenger.isDestinationUpward() != elevator.isDirectionUp()
                    || elevator.isFull()) {

                if (passenger.getInitialStory() == currentStorey.getLevel()
                        && passenger.isDestinationUpward() != elevator.isDirectionUp()) {
                    ignorePassengersCount.incrementAndGet();
                }

                storyConditions[passenger.getInitialStory()].await();
            }
        } catch (InterruptedException e) {
            logger.catching(e);
        }
    }

    /**
     * Boards specified passenger to elevator. If current storey dispatch is empty, increments corresponding counter.
     *
     * @param passenger passenger to be embarked to elevators cab.
     */
    public void embarkPassenger(Passenger passenger) {
        Objects.requireNonNull(passenger);
        logger.info(MessageConstants.BOARDING_OF_PASSENGER, passenger.getId(), currentStorey.getLevel());
        currentStorey.getDispatchContainer().remove(passenger);
        elevator.boardPassenger(passenger);
        if (currentStorey.getDispatchContainer().isEmpty()) {
            emptyDispatch.add(currentStorey.getLevel());
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
            while (passenger.getDestinationStory() != currentStorey.getLevel()) {
                transported.await();
            }
        } catch (InterruptedException e) {
            logger.catching(e);
        }
    }

    /**
     * Removes passenger from elevator when he is transported to his destination storey.
     *
     * @param passenger passenger to deboard from elevator.
     */
    public void disembarkPassenger(Passenger passenger) {
        Objects.requireNonNull(passenger);
        logger.info(MessageConstants.UNBOARDING_OF_PASSENGER, passenger.getId(), currentStorey.getLevel());
        elevator.deboardPassenger(passenger);
        currentStorey.getArrivalContainer().add(passenger);
    }
}
