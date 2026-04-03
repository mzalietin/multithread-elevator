package by.training.elevator;

import by.training.elevator.conf.Configuration;
import by.training.elevator.entity.building.Building;
import by.training.elevator.entity.building.Controller;
import by.training.elevator.entity.building.Elevator;
import by.training.elevator.entity.building.Level;
import by.training.elevator.entity.passenger.Passenger;
import by.training.elevator.entity.passenger.PassengerFactory;
import java.util.ArrayList;
import java.util.List;

public final class Initializer {

    public static Controller initSimulation() {
        int levelsCount = Configuration.LEVELS_COUNT;

        List<Level> levels = new ArrayList<>(levelsCount);

        for (int i = 0; i < levelsCount; i++) {
            levels.add(new Level(i));
        }

        Passenger passenger;
        for (int i = 0; i < Configuration.PASSENGERS_NUMBER; i++) {
            passenger = PassengerFactory.producePassenger();
            levels.get(passenger.getInitialLevel()).getDispatchContainer().add(passenger);
        }

        Building building = new Building(levels);
        Elevator elevator = new Elevator(Configuration.ELEVATOR_CAPACITY, building);
        Controller controller = new Controller(levelsCount, elevator);

        return controller;
    }
}
