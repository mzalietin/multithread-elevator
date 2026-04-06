package by.training.elevator.entity.building;

import by.training.elevator.entity.passenger.Passenger;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class Level {
    private final int value;
    private final Set<Passenger> departureUpGroup;
    private final Set<Passenger> departureDownGroup;
    private final Set<Passenger> arrivalContainer;

    public Level(int value) {
        if (value >= 0) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Level value cannot be negative");
        }
        this.departureUpGroup = new HashSet<>();
        this.arrivalContainer = new HashSet<>();
        this.departureDownGroup = new HashSet<>();
    }

    public int getValue() {
        return value;
    }

    public void addForDeparture(Passenger passenger) {
        if (passenger.isDestinationUpward()) {
            this.departureUpGroup.add(passenger);
        } else {
            this.departureDownGroup.add(passenger);
        }
    }

    public void removeFromDeparture(Passenger passenger) {
        if (passenger.isDestinationUpward()) {
            this.departureUpGroup.remove(passenger);
        } else {
            this.departureDownGroup.remove(passenger);
        }
    }

    public Stream<Passenger> remainingPassengers() {
        return Stream.concat(this.departureUpGroup.stream(), this.departureDownGroup.stream());
    }

    public boolean isDepartureEmpty() {
        return this.departureUpGroup.isEmpty() && this.departureDownGroup.isEmpty();
    }

    public void addToArrival(Passenger passenger) {
        arrivalContainer.add(passenger);
    }

    public Set<Passenger> arrivedPassengers() {
        return new HashSet<>(arrivalContainer);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        final Level level = (Level) o;
        return value == level.value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
