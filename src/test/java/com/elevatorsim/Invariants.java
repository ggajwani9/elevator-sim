package com.elevatorsim;

import com.elevatorsim.model.Passenger;
import com.elevatorsim.model.SimResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rules every run must obey, checked from the output alone. */
public final class Invariants {

    private Invariants() {
    }

    public static void check(SimResult r) {
        int cars = r.config().elevators();

        // Positions: start at the lobby, stay in the building, at most one floor per tick.
        for (int t = 0; t < r.positions().size(); t++) {
            for (int car = 0; car < cars; car++) {
                int f = r.floorAt(t, car);
                assertTrue(f >= 1 && f <= r.config().floors(), "outside building: car " + car + " tick " + t);
                if (t == 0) {
                    assertEquals(1, f, "car " + car + " must start at the lobby");
                } else {
                    assertTrue(Math.abs(f - r.floorAt(t - 1, car)) <= 1, "jumped: car " + car + " tick " + t);
                }
            }
        }

        int[][] load = new int[cars][r.positions().size() + 1];
        int last = 0;
        for (Passenger p : r.passengers()) {
            assertTrue(p.isDelivered(), () -> "never delivered: " + p);                        // serve everyone
            assertTrue(p.pickupTime() >= p.requestTime(), () -> "picked up early: " + p);
            assertEquals(p.source(), r.floorAt(p.pickupTime(), p.elevatorId()), () -> "not at source: " + p);
            assertEquals(p.dest(), r.floorAt(p.dropoffTime(), p.elevatorId()), () -> "not at dest: " + p);
            assertEquals(Math.abs(p.dest() - p.source()), p.travelTime(), () -> "ride not direct: " + p); // direction logic
            for (int t = p.pickupTime(); t < p.dropoffTime(); t++) {
                load[p.elevatorId()][t]++;
            }
            last = Math.max(last, p.dropoffTime());
        }
        for (int car = 0; car < cars; car++) {                                                   // capacity
            for (int t = 0; t < load[car].length; t++) {
                assertTrue(load[car][t] <= r.config().capacity(), "over capacity: car " + car + " tick " + t);
            }
        }
        assertEquals(last, r.lastTick(), "log must end at the last drop-off");
    }
}
