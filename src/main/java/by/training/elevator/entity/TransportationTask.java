package by.training.elevator.entity;

import by.training.elevator.entity.building.Controller;
import by.training.elevator.entity.passenger.Passenger;
import by.training.elevator.entity.passenger.TransportationState;
import java.util.Objects;

public class TransportationTask implements Runnable {
    private final Passenger passenger;
    private final Controller controller;

    public TransportationTask(Passenger passenger, Controller controller) {
        this.passenger = Objects.requireNonNull(passenger);
        this.controller = Objects.requireNonNull(controller);
    }

    @Override
    public void run() {
        controller.awaitBoarding(passenger);
        controller.boardPassenger(passenger);
        controller.awaitTransportation(passenger);
        controller.unboardPassenger(passenger);
        passenger.setState(TransportationState.COMPLETED);
    }
}
