package by.training.elevator.entity.building;

import by.training.elevator.entity.passenger.Passenger;
import java.util.Comparator;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class Elevator {
    private final Building building;
    private final SortedSet<Passenger> cabin;
    private final int capacity;

    public Elevator(int capacity, Building building) {
        this.building = Objects.requireNonNull(building);
        this.capacity = capacity;
        cabin = new ConcurrentSkipListSet<>(Comparator.comparingInt(Passenger::getDestinationLevel));
    }

    public Building getBuilding() {
        return building;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return cabin.isEmpty();
    }

    public boolean isFull() {
        return capacity == cabin.size();
    }

    public void boardPassenger(Passenger passenger) {
        cabin.add(passenger);
    }

    public void unboardPassenger(Passenger passenger) {
        cabin.remove(passenger);
    }

    public SortedSet<Passenger> getCabin() {
        return cabin;
    }

    public OptionalInt closestDestination(boolean directionUp, int currentLevel) {
        if (cabin.isEmpty()) {
            return OptionalInt.empty();
        } else if (directionUp) {
            int destLevel = cabin.first().getDestinationLevel();
            return destLevel > currentLevel ? OptionalInt.of(destLevel) : OptionalInt.empty();
        } else {
            int destLevel = cabin.last().getDestinationLevel();
            return destLevel < currentLevel ? OptionalInt.of(destLevel) : OptionalInt.empty();
        }
    }
}
