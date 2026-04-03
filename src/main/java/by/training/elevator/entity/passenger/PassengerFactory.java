package by.training.elevator.entity.passenger;

import by.training.elevator.conf.Configuration;

public class PassengerFactory {

    private static int idSource = 0;

    public static Passenger producePassenger() {
        int init = generateLevel();
        int dest = generateLevel();

        while (dest == init) {
            dest = generateLevel();
        }

        return new Passenger(++idSource, init, dest);
    }

    private static int generateLevel() {
        return (int) (Math.random() * Configuration.LEVELS_COUNT);
    }
}
