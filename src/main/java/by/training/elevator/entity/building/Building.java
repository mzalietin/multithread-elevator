package by.training.elevator.entity.building;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Building {
    private final List<Level> levels;

    public Building(List<Level> levels) {
        this.levels = Collections.unmodifiableList(Objects.requireNonNull(levels));
    }

    public List<Level> getLevels() {
        return levels;
    }
}
