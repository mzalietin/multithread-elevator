package by.training.elevator;

import by.training.elevator.entity.building.Controller;

public class Runner {

    public static void main(String[] args) {
        Controller controller = Initializer.initSimulation();
        controller.startTransportation();
        controller.validateFinish();
    }
}

