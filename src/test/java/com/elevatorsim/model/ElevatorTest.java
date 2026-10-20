package com.elevatorsim.model;

import com.elevatorsim.config.SimConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The car's rules, tick by tick. */
class ElevatorTest {

    private final Elevator car = new Elevator(0, SimConfig.of(1, 10, 2));

    private Passenger call(int time, String id, int from, int to) {
        Passenger p = new Passenger(new Request(time, id, from, to));
        car.assign(p);
        return p;
    }

    private void run(int fromTick, int toTick) {
        for (int t = fromTick; t <= toTick; t++) {
            car.step(t);
        }
    }

    @Test
    @DisplayName("with nothing to do the car stays put")
    void idle() {
        run(0, 3);
        assertEquals(1, car.floor());
    }

    @Test
    @DisplayName("board on the same tick, then one floor per tick")
    void boardAndRide() {
        Passenger p = call(0, "a", 1, 4);
        run(0, 3);
        assertEquals(0, p.pickupTime());
        assertEquals(3, p.dropoffTime());
        assertEquals(0, p.waitTime());
        assertEquals(3, p.travelTime());
        assertEquals(3, p.totalTime());
    }

    @Test
    @DisplayName("going up, the car passes someone going down and comes back for them")
    void directionLogic() {
        Passenger up = call(0, "up", 1, 6);
        Passenger down = call(0, "down", 3, 1);
        run(0, 5);
        assertEquals(5, up.dropoffTime());
        assertFalse(down.pickupTime() >= 0, "not picked up on the way up");
        run(6, 10);
        assertEquals(8, down.pickupTime());
        assertEquals(10, down.dropoffTime());
    }

    @Test
    @DisplayName("capacity: the oldest board first, the rest wait")
    void capacity() {
        Passenger a = call(0, "a", 1, 3);
        Passenger b = call(0, "b", 1, 3);
        Passenger c = call(0, "c", 1, 3);
        car.step(0);
        assertEquals(0, a.pickupTime());
        assertEquals(0, b.pickupTime());
        assertEquals(-1, c.pickupTime());
    }

    @Test
    @DisplayName("assignment is final")
    void assignmentIsFinal() {
        Passenger p = call(0, "a", 1, 3);
        assertThrows(IllegalStateException.class, () -> new Elevator(1, SimConfig.of(2, 10, 2)).assign(p));
    }

    @Test
    @DisplayName("look-ahead is exact and changes nothing")
    void lookAhead() {
        call(0, "a", 1, 6);                                           // total 5
        Passenger extra = new Passenger(new Request(0, "b", 4, 2));   // picked up on the way back at t=7, off at t=9
        assertEquals(5, car.predictTotalTime(0, null));
        assertEquals(14, car.predictTotalTime(0, extra));
        assertEquals(1, car.outstanding());
        assertEquals(1, car.floor());
        assertFalse(extra.isAssigned());
    }
}
