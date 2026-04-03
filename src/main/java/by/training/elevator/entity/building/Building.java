package by.training.elevator.entity.building;

import by.training.elevator.conf.Configuration;
import by.training.elevator.entity.passenger.Passenger;
import by.training.elevator.entity.TransportationTask;
import by.training.elevator.entity.passenger.TransportationState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Building {
    private List<Storey> stories;
    private Elevator elevator;

    public Building(List<Storey> stories) {
        Objects.requireNonNull(stories);
        this.stories = Collections.unmodifiableList(new ArrayList<>(stories));
    }

    public void setElevator(Elevator elevator) {
        this.elevator = Objects.requireNonNull(elevator);
    }

    public Elevator getElevator() {
        return elevator;
    }

    public List<Storey> getStories() {
        return Collections.unmodifiableList(new ArrayList<>(stories));
    }
}
