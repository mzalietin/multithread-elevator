package by.training.elevator.conf;

import java.util.ResourceBundle;

public interface Configuration {
    ResourceBundle BUNDLE = ResourceBundle.getBundle("config");

    int LEVELS_COUNT = Integer.valueOf(BUNDLE.getString("levelsNumber"));
    int ELEVATOR_CAPACITY = Integer.valueOf(BUNDLE.getString("elevatorCapacity"));
    int PASSENGERS_NUMBER = Integer.valueOf(BUNDLE.getString("passengersNumber"));
}
