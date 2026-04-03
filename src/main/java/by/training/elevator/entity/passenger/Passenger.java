package by.training.elevator.entity.passenger;

public class Passenger {
    private final int id;
    private final int initialLevel;
    private final int destinationLevel;
    private TransportationState state;

    Passenger(int id, int initialLevel, int destinationLevel) {
        this.id = id;
        this.initialLevel = initialLevel;
        this.destinationLevel = destinationLevel;
        state = TransportationState.NOT_STARTED;
    }

    public int getId() {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Passenger passenger = (Passenger) o;

        if (id != passenger.id) return false;
        if (initialLevel != passenger.initialLevel) return false;
        return destinationLevel == passenger.destinationLevel;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + initialLevel;
        result = 31 * result + destinationLevel;
        return result;
    }
}
