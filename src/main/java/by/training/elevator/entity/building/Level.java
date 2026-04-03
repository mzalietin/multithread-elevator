package by.training.elevator.entity.building;

import by.training.elevator.entity.passenger.Passenger;

import java.util.HashSet;
import java.util.Set;

public class Level {
    private final int value;
    private final Set<Passenger> dispatchContainer;
    private final Set<Passenger> arrivalContainer;

    public Level(int value) {
        if (value >= 0) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Level value cannot be negative");
        }
        this.dispatchContainer = new HashSet<>();
        this.arrivalContainer = new HashSet<>();
    }

    public int getValue() {
        return value;
    }

    public Set<Passenger> getDispatchContainer() {
        return dispatchContainer;
    }

    public Set<Passenger> getArrivalContainer() {
        return arrivalContainer;
    }
}
