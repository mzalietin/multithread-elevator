package by.training.elevator.entity.passenger;

import java.util.Objects;

public class Passenger {
    private final String id;
    private final int initialLevel;
    private final int destinationLevel;
    private TransportationState state;

    public Passenger(String id, int initialLevel, int destinationLevel) {
        this.id = id;
        this.initialLevel = initialLevel;
        this.destinationLevel = destinationLevel;
        state = TransportationState.NOT_STARTED;
    }

    public String getId() {
        return id;
    }

    public int getDestinationLevel() {
        return destinationLevel;
    }

    public int getInitialLevel() {
        return initialLevel;
    }

    public TransportationState getState() {
        return state;
    }

    public void setState(TransportationState state) {
        this.state = state;
    }

    public boolean isDestinationUpward() {
        return destinationLevel > initialLevel;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        final Passenger passenger = (Passenger) o;
        return initialLevel == passenger.initialLevel && destinationLevel == passenger.destinationLevel && Objects.equals(id,
                passenger.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, initialLevel, destinationLevel);
    }

    @Override
    public String toString() {
        return "Passenger{" +
                "id='" + id + '\'' +
                ", initialLevel=" + initialLevel +
                ", destinationLevel=" + destinationLevel +
                ", state=" + state +
                '}';
    }
}
