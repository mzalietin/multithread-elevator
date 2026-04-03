package by.training.elevator.entity.building;

import by.training.elevator.entity.passenger.Passenger;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Elevator {
    private final Building building;
    private final Set<Passenger> container;
    private final int capacity;

    public Elevator(int capacity, Building building) {
        this.building = Objects.requireNonNull(building);
        if (capacity >= 0) {
            this.capacity = capacity;
        } else {
            throw new IllegalArgumentException("Elevator capacity cannot be negative");
        }
        container = new HashSet<>(capacity, 1.0f);
    }

    public Building getBuilding() {
        return building;
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

    public void unboardPassenger(Passenger passenger) {
        container.remove(passenger);
    }
}
