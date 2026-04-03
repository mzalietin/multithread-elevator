package by.training.elevator.entity;

import by.training.elevator.entity.building.Controller;
import by.training.elevator.entity.passenger.Passenger;

import java.util.Objects;
import java.util.concurrent.Callable;

import by.training.elevator.entity.passenger.TransportationState;
import org.apache.logging.log4j.LogManager;

public class TransportationTask implements Callable<Void> {
    private Passenger passenger;
    private Controller controller;

    public TransportationTask(Passenger passenger, Controller controller) {
        this.passenger = Objects.requireNonNull(passenger);
        this.controller = Objects.requireNonNull(controller);

        passenger.setState(TransportationState.IN_PROGRESS);
    }

    @Override
    public Void call() {
        try {
            controller.acquireLock();
            controller.awaitBoarding(passenger);
            controller.embarkPassenger(passenger);
            controller.awaitTransportation(passenger);
            controller.disembarkPassenger(passenger);
        } finally {
            controller.releaseLock();
        }
        passenger.setState(TransportationState.COMPLETED);
        return null;
    }
}
