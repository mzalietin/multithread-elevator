package by.training.elevator.entity.building;

import by.training.elevator.entity.TransportationTask;
import by.training.elevator.entity.passenger.Passenger;
import org.apache.logging.log4j.LogManager;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;


public class Elevator {
    private Building building;
    private Controller controller;

    private Set<Passenger> container;
    private int capacity;
    private boolean directionUp;

    public Elevator(int capacity) {
        if (capacity >= 0) {
            this.capacity = capacity;
        } else {
            throw new IllegalArgumentException("Elevator capacity cannot be negative");
        }
        container = new HashSet<>(capacity, 1.0f);
        directionUp = true;
    }

    public void setBuilding(Building building) {
        this.building = Objects.requireNonNull(building);
    }

    public Building getBuilding() {
        return building;
    }

    public void setController(Controller controller) {
        this.controller = Objects.requireNonNull(controller);
    }

    public Controller getController() {
        return controller;
    }

    public void setDirectionUp(boolean directionUp) {
        this.directionUp = directionUp;
    }

    public boolean isDirectionUp() {
        return directionUp;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return container.isEmpty();
    }

    public boolean isFull() {
        return capacity == container.size();
    }

    public void boardPassenger(Passenger passenger) {
        container.add(passenger);
    }

    public void deboardPassenger(Passenger passenger) {
        container.remove(passenger);
    }
}
