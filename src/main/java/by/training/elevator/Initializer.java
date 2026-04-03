package by.training.elevator;

import by.training.elevator.conf.Configuration;
import by.training.elevator.entity.building.Building;
import by.training.elevator.entity.building.Controller;
import by.training.elevator.entity.building.Elevator;
import by.training.elevator.entity.building.Storey;
import by.training.elevator.entity.passenger.Passenger;
import by.training.elevator.entity.passenger.PassengerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class Initializer {

    public static Building initBuilding() {
        int storeysCount = Configuration.STOREYS_COUNT;

        List<Storey> storeys = new ArrayList<>(storeysCount);

        for (int i = 0; i < storeysCount; i++) {
            storeys.add(new Storey(i));
        }

        Passenger passenger;
        for (int i = 0; i < Configuration.PASSENGERS_NUMBER; i++) {
            passenger = PassengerFactory.producePassenger();
            storeys.get(passenger.getInitialStory()).getDispatchContainer().add(passenger);
        }

        Building building = new Building(storeys);
        Elevator elevator = new Elevator(Configuration.ELEVATOR_CAPACITY);

        building.setElevator(elevator);
        elevator.setBuilding(building);

        Controller controller = new Controller(storeysCount);

        elevator.setController(controller);
        controller.setElevator(elevator);

        return building;
    }
}
