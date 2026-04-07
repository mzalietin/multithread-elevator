package by.training.elevator.entity.passenger;

import by.training.elevator.conf.Configuration;
import java.util.Random;

public class PassengerFactory {

    private static final Random random = new Random();

    public static Passenger producePassenger() {
        int init = generateLevel();
        int dest = generateLevel();

        while (dest == init) {
            dest = generateLevel();
        }

        return new Passenger(String.format("pas-%d", random.nextInt(10_000)), init, dest);
    }

    private static int generateLevel() {
        return random.nextInt(Configuration.LEVELS_COUNT);
    }
}
