package by.training.elevator.entity.building;

import by.training.elevator.entity.passenger.Passenger;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Storey {
    private int level;
    private final Set<Passenger> dispatchContainer;
    private final Set<Passenger> arrivalContainer;

    public Storey(int level) {
        if (level >= 0) {
            this.level = level;
        } else {
            throw new IllegalArgumentException("Storey level cannot be negative");
        }
        this.dispatchContainer = new HashSet<>();
        this.arrivalContainer = new HashSet<>();
    }

    public int getLevel() {
        return level;
    }

    public Set<Passenger> getDispatchContainer() {
        return dispatchContainer;
    }

    public Set<Passenger> getArrivalContainer() {
        return arrivalContainer;
    }
}
