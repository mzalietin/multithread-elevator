package by.training.elevator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            for (Passenger pas: st.getDispatchContainer()) {
                int id = pas.getId();
                int initLevel = pas.getInitialLevel();
                int destLevel = pas.getDestinationLevel();

                assertTrue(destLevel >= 0 && destLevel < Configuration.LEVELS_COUNT);
                assertNotEquals(initLevel, destLevel);
                assertEquals(TransportationState.NOT_STARTED, pas.getState());
            }
        }
    }

    @Test
    public void testLevels() {
        List<Level> levelList = building.getLevels();

        for (Level st: levelList) {
            Set<Passenger> passengers = st.getDispatchContainer();

            for (Passenger pas: passengers) {
                assertEquals(levelList.indexOf(st), pas.getInitialLevel());
            }

            assertTrue(st.getArrivalContainer().isEmpty());
        }
    }

    @Test
    public void testElevator() {
        assertEquals(Configuration.ELEVATOR_CAPACITY, elevator.getCapacity());
        assertTrue(elevator.isDirectionUp());
        assertTrue(elevator.isEmpty());
        assertNotNull(elevator.getBuilding());
    }

    @Test
    public void testController() {
        final int FIRST_LEVEL = 0;
        assertEquals(FIRST_LEVEL, controller.getCurrentLevel());

        Passenger passenger = mock(Passenger.class);
        when(passenger.getInitialLevel()).thenReturn(FIRST_LEVEL);
        when(passenger.getDestinationLevel()).thenReturn(FIRST_LEVEL);

        controller.boardPassenger(passenger);

        assertFalse(building.getLevels()
                .get(passenger.getInitialLevel())
                .getDispatchContainer()
                .contains(passenger));
        assertFalse(elevator.isEmpty());

        controller.unboardPassenger(passenger);

        assertTrue(building.getLevels()
                .get(passenger.getDestinationLevel())
                .getArrivalContainer()
                .contains(passenger));
        assertTrue(elevator.isEmpty());
    }
}
