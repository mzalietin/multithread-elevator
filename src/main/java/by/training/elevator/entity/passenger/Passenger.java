package by.training.elevator.entity.passenger;

public class Passenger {
    private int id;
    private int initialStory;
    private int destinationStory;
    private TransportationState state;

    Passenger(int id, int initialStory, int destinationStory) {
        this.id = id;
        this.initialStory = initialStory;
        this.destinationStory = destinationStory;
        state = TransportationState.NOT_STARTED;
    }

    public int getId() {
        return id;
    }

    public int getDestinationStory() {
        return destinationStory;
    }

    public int getInitialStory() {
        return initialStory;
    }

    public TransportationState getState() {
        return state;
    }

    public void setState(TransportationState state) {
        this.state = state;
    }

    public boolean isDestinationUpward() {
        return destinationStory > initialStory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Passenger passenger = (Passenger) o;

        if (id != passenger.id) return false;
        if (initialStory != passenger.initialStory) return false;
        return destinationStory == passenger.destinationStory;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + initialStory;
        result = 31 * result + destinationStory;
        return result;
    }
}
