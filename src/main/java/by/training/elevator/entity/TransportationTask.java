package by.training.elevator.entity;

import by.training.elevator.entity.building.Controller;
import by.training.elevator.entity.passenger.Passenger;

import java.util.Objects;
import java.util.concurrent.Callable;

import by.training.elevator.entity.passenger.TransportationState;

public class TransportationTask implements Callable<Void> {
    private final Passenger passenger;
    private final Controller controller;

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
            controller.boardPassenger(passenger);
            controller.awaitTransportation(passenger);
            controller.unboardPassenger(passenger);
        } finally {
            controller.releaseLock();
        }
        passenger.setState(TransportationState.COMPLETED);
        return null;
    }
}
