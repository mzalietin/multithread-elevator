package by.training.elevator;

import by.training.elevator.Initializer;
import by.training.elevator.entity.building.Building;
import by.training.elevator.entity.building.Controller;

public class Runner {

    public static void main(String[] args) {
        Building building = Initializer.initBuilding();
        Controller controller = building.getElevator().getController();
        controller.startTransportation();
        controller.validateFinish();
    }
}

