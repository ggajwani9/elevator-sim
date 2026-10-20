package com.elevatorsim.service;

import com.elevatorsim.Invariants;
import com.elevatorsim.config.SimConfig;
import com.elevatorsim.model.Elevator;
import com.elevatorsim.model.Passenger;
import com.elevatorsim.model.Request;
import com.elevatorsim.model.SimResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.elevatorsim.TestData.brief;
import static com.elevatorsim.TestData.req;
import static com.elevatorsim.TestData.walkthrough;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hand-checked scenarios. */
class SimulationServiceTest {

    private final SimulationService service = new SimulationService();

    private static void assertTrip(SimResult r, String id, int car, int pickup, int dropoff) {
        Passenger p = r.passenger(id);
        assertEquals(car, p.elevatorId(), id + " car");
        assertEquals(pickup, p.pickupTime(), id + " pickup");
        assertEquals(dropoff, p.dropoffTime(), id + " drop-off");
    }

    @Test
    @DisplayName("brief, 1 elevator: passenger3 waits for the car to finish going up")
    void briefOneElevator() {
        SimResult r = service.run(brief(), SimConfig.of(1, 51, 10));
        assertTrip(r, "passenger1", 0, 0, 50);
        assertTrip(r, "passenger2", 0, 0, 36);
        assertTrip(r, "passenger3", 0, 81, 100);
        assertEquals(100, r.lastTick());
        Invariants.check(r);
    }

    @Test
    @DisplayName("brief, 2 elevators: the idle car takes passenger3")
    void briefTwoElevators() {
        SimResult r = service.run(brief(), SimConfig.of(2, 51, 10));
        assertTrip(r, "passenger1", 0, 0, 50);
        assertTrip(r, "passenger2", 0, 0, 36);
        assertTrip(r, "passenger3", 1, 29, 48);
        assertEquals(50, r.lastTick());
        Invariants.check(r);
    }

    @Test
    @DisplayName("walkthrough: assignments, costs, positions and timings as presented")
    void walkthroughScenario() {
        SimConfig config = SimConfig.of(2, 10, 4);
        Scheduler dispatch = Scheduler.create(config);
        List<String> costs = new ArrayList<>();
        Scheduler recordCosts = (Passenger p, List<Elevator> cars, int tick) -> {
            long car0 = cars.get(0).predictTotalTime(tick, p) - cars.get(0).predictTotalTime(tick, null);
            long car1 = cars.get(1).predictTotalTime(tick, p) - cars.get(1).predictTotalTime(tick, null);
            costs.add(p.id() + " " + car0 + "/" + car1);
            return dispatch.choose(p, cars, tick);
        };

        SimResult r = service.run(walkthrough(), config, recordCosts);

        assertEquals(List.of("Alice 7/7", "Bob 4/4", "Carol 10/11", "Dan 6/8"), costs);
        assertTrip(r, "Alice", 0, 0, 7);
        assertTrip(r, "Bob", 0, 0, 4);
        assertTrip(r, "Carol", 0, 10, 15);
        assertTrip(r, "Dan", 0, 5, 8);
        int[] car0 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 8, 7, 6, 5, 4, 3, 2};
        for (int t = 0; t <= 15; t++) {
            assertArrayEquals(new int[] {car0[t], 1}, r.positions().get(t), "tick " + t);
        }
        Invariants.check(r);
    }

    @Test
    @DisplayName("walkthrough: without Dan, Carol would finish 2 ticks sooner")
    void walkthroughWithoutDan() {
        SimResult r = service.run(walkthrough().subList(0, 3), SimConfig.of(2, 10, 4));
        assertEquals(10, r.passenger("Carol").totalTime());
    }

    @Test
    @DisplayName("the clock ticks through gaps; an idle car stays put")
    void gaps() {
        SimResult r = service.run(List.of(req(0, "a", 1, 3), req(20, "b", 1, 2)), SimConfig.of(1, 10, 4));
        assertTrip(r, "b", 0, 22, 23);
        assertEquals(24, r.positions().size());
        assertEquals(3, r.floorAt(20, 0));
        Invariants.check(r);
    }

    @Test
    @DisplayName("a call where the car is standing has zero wait")
    void zeroWait() {
        SimResult r = service.run(List.of(req(5, "a", 1, 3)), SimConfig.of(1, 10, 4));
        assertEquals(0, r.passenger("a").waitTime());
    }

    @Test
    @DisplayName("empty input: one row, every car at the lobby")
    void empty() {
        SimResult r = service.run(List.of(), SimConfig.of(3, 10, 4));
        assertEquals(1, r.positions().size());
        assertArrayEquals(new int[] {1, 1, 1}, r.positions().get(0));
    }

    @Test
    @DisplayName("invalid input is rejected and the request is named")
    void invalidInput() {
        SimConfig c = SimConfig.of(1, 10, 4);
        for (List<Request> bad : List.of(
                List.of(req(0, "x", 3, 3)),
                List.of(req(0, "x", 1, 11)),
                List.of(req(0, "x", 0, 5)),
                List.of(req(-1, "x", 1, 5)),
                List.of(req(0, "x", 1, 5), req(1, "x", 5, 1)))) {
            InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> service.run(bad, c));
            assertTrue(e.getMessage().startsWith("x"), e.getMessage());
        }
    }

    @Test
    @DisplayName("a runaway run stops at maxTicks and says who was left")
    void maxTicks() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.run(List.of(req(0, "stranded", 1, 51)), SimConfig.of(1, 60, 4).withMaxTicks(10)));
        assertTrue(e.getMessage().contains("stranded"));
    }

    @Test
    @DisplayName("passengers come back in input order")
    void inputOrder() {
        SimResult r = service.run(List.of(req(9, "z", 4, 1), req(0, "a", 1, 4)), SimConfig.of(2, 10, 4));
        assertEquals(List.of("z", "a"), r.passengers().stream().map(Passenger::id).toList());
    }
}
