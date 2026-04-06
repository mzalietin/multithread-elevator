package by.training.elevator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import by.training.elevator.conf.Configuration;
import by.training.elevator.entity.building.Building;
import by.training.elevator.entity.building.Controller;
import by.training.elevator.entity.building.Elevator;
import by.training.elevator.entity.building.Level;
import by.training.elevator.entity.passenger.Passenger;
import by.training.elevator.entity.passenger.TransportationState;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class InitializerTest {

    private Controller controller;
    private Elevator elevator;
    private Building building;

    @Before
    public void setUp() throws Exception {
        controller = Initializer.initSimulation();
        elevator = controller.getElevator();
        building = elevator.getBuilding();
    }

    @Test
    public void testPassengers() {
        for (Level st: building.getLevels()) {
            st.remainingPassengers().forEach(pas -> {
                int id = pas.getId();
                int initLevel = pas.getInitialLevel();
                int destLevel = pas.getDestinationLevel();

                assertTrue(destLevel >= 0 && destLevel < Configuration.LEVELS_COUNT);
                assertNotEquals(initLevel, destLevel);
                assertEquals(TransportationState.NOT_STARTED, pas.getState());
            });
            assertTrue(st.arrivedPassengers().isEmpty());
        }
    }

    @Test
    public void testElevator() {
        assertEquals(Configuration.ELEVATOR_CAPACITY, elevator.getCapacity());
        assertTrue(elevator.isEmpty());
        assertNotNull(elevator.getBuilding());
    }

    @Test
    public void testController() {
        //todo fix
//        final int FIRST_LEVEL = 0;
//        assertEquals(FIRST_LEVEL, controller.getCurrentLevel());
//
//        Passenger passenger = new Passenger(1, FIRST_LEVEL, FIRST_LEVEL);
//
//        controller.boardPassenger(passenger);
//
//        assertFalse(building.getLevels()
//                .get(passenger.getInitialLevel())
//                .remainingPassengers()
//                .findAny()
//                .isPresent());
//        assertFalse(elevator.isEmpty());
//
//        controller.unboardPassenger(passenger);
//
//        assertTrue(building.getLevels()
//                .get(passenger.getDestinationLevel())
//                .arrivedPassengers()
//                .contains(passenger));
//        assertTrue(elevator.isEmpty());
    }
}
