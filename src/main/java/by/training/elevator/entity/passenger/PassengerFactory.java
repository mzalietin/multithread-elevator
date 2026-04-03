package by.training.elevator.entity.passenger;

import by.training.elevator.conf.Configuration;

public class PassengerFactory {

    private static int idSource = 0;

    public static Passenger producePassenger() {
        int init = generateStory();
        int dest = generateStory();

        while (dest == init) {
            dest = generateStory();
        }

        return new Passenger(++idSource, init, dest);
    }

    private static int generateStory() {
        return (int) (Math.random() * Configuration.STOREYS_COUNT);
    }
}
