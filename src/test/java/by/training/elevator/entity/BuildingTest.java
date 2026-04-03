package by.training.elevator.entity;

import by.training.elevator.conf.Configuration;
import by.training.elevator.Initializer;
import by.training.elevator.entity.building.Building;
import by.training.elevator.entity.building.Controller;
import by.training.elevator.entity.building.Elevator;
import by.training.elevator.entity.building.Storey;
import by.training.elevator.entity.passenger.Passenger;
import by.training.elevator.entity.passenger.PassengerFactory;
import by.training.elevator.entity.passenger.TransportationState;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class BuildingTest {

    private Building building;

    @Before
    public void setUp() throws Exception {
        building = Initializer.initBuilding();
    }

    @Test
    public void testPassengers() {
        for (Storey st: building.getStories()) {
            for (Passenger pas: st.getDispatchContainer()) {
                int id = pas.getId();
                int initStory = pas.getInitialStory();
                int destStory = pas.getDestinationStory();

                assertTrue(destStory >= 0 && destStory < Configuration.STOREYS_COUNT);
                assertNotEquals(initStory, destStory);
                assertEquals(TransportationState.NOT_STARTED, pas.getState());
            }
        }
    }

    @Test
    public void testStories() {
        List<Storey> storeyList = building.getStories();

        for (Storey st: storeyList) {
            Set<Passenger> passengers = st.getDispatchContainer();

            for (Passenger pas: passengers) {
                assertEquals(storeyList.indexOf(st), pas.getInitialStory());
            }

            assertTrue(st.getArrivalContainer().isEmpty());
        }
    }

    @Test
    public void testElevator() {
        Elevator elevator = building.getElevator();

        assertEquals(Configuration.ELEVATOR_CAPACITY, elevator.getCapacity());
        assertTrue(elevator.isDirectionUp());
        assertTrue(elevator.isEmpty());

        assertNotNull(elevator.getBuilding());
        assertNotNull(elevator.getController());
    }

    @Test
    public void testController() {
        Controller controller = building.getElevator().getController();

        final int FIRST_STOREY_LEVEL = 0;
        assertEquals(FIRST_STOREY_LEVEL, controller.getCurrentStorey());

        Passenger passenger = mock(Passenger.class);
        when(passenger.getInitialStory()).thenReturn(FIRST_STOREY_LEVEL);
        when(passenger.getDestinationStory()).thenReturn(FIRST_STOREY_LEVEL);

        controller.embarkPassenger(passenger);

        assertFalse(building.getStories()
                .get(passenger.getInitialStory())
                .getDispatchContainer()
                .contains(passenger));
        assertFalse(building.getElevator().isEmpty());

        controller.disembarkPassenger(passenger);

        assertTrue(building.getStories()
                .get(passenger.getDestinationStory())
                .getArrivalContainer()
                .contains(passenger));
        assertTrue(building.getElevator().isEmpty());
    }
}
