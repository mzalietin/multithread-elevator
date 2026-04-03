package by.training.elevator.entity.building;

import by.training.elevator.entity.passenger.Passenger;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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
        container = new TreeSet<>(Comparator.comparingInt(Passenger::getDestinationLevel));
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
