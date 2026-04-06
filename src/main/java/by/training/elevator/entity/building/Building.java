package by.training.elevator.entity.building;

import by.training.elevator.entity.passenger.Passenger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Building {
    private final List<Level> levels;

    public Building(List<Level> levels) {
        this.levels = Collections.unmodifiableList(Objects.requireNonNull(levels));
    }

    public List<Level> getLevels() {
        return levels;
    }

    public List<Passenger> allPassengers() {
        return levels.stream().flatMap(Level::remainingPassengers).collect(Collectors.toList());
    }
}
