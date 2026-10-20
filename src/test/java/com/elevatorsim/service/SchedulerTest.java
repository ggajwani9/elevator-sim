package com.elevatorsim.service;

import com.elevatorsim.config.SchedulerType;
import com.elevatorsim.config.SimConfig;
import com.elevatorsim.model.Request;
import com.elevatorsim.model.SimResult;
import com.elevatorsim.utils.Stats;
import com.elevatorsim.utils.TrafficGenerator;
import com.elevatorsim.utils.TrafficGenerator.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static com.elevatorsim.TestData.req;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerTest {

    private final SimulationService service = new SimulationService();

    private SimResult run(SchedulerType type, int elevators, int floors, Request... rs) {
        return service.run(List.of(rs), SimConfig.of(elevators, floors, 8).withScheduler(type));
    }

    @Test
    @DisplayName("nearest car: closest to the caller, ties to the lower id")
    void nearestCar() {
        SimResult r = run(SchedulerType.NEAREST_CAR, 2, 10,
                req(0, "a", 1, 10),     // tie -> car 0
                req(5, "b", 8, 9),      // car 0 is at 6, car 1 at 1
                req(5, "c", 2, 1));     // car 1 is one floor away
        assertEquals(List.of(0, 0, 1), r.passengers().stream().map(p -> p.elevatorId()).toList());
    }

    @Test
    @DisplayName("dispatch: a trip that costs the same on both cars goes to the busy one")
    void dispatchPrefersBusyCar() {
        SimResult r = run(SchedulerType.DISPATCH, 2, 20, req(0, "a", 1, 10), req(0, "b", 1, 8));
        assertEquals(0, r.passenger("b").elevatorId());
    }

    @Test
    @DisplayName("dispatch: sends the idle car rather than make a busy one detour")
    void dispatchAvoidsDetour() {
        SimResult r = run(SchedulerType.DISPATCH, 2, 30, req(0, "a", 1, 30), req(0, "b", 20, 1));
        assertEquals(1, r.passenger("b").elevatorId());
        assertEquals(38, r.passenger("b").totalTime());   // car 0 would have made it 58
    }

    @ParameterizedTest
    @EnumSource(Pattern.class)
    @DisplayName("dispatch beats nearest car on average total time")
    void dispatchWins(Pattern pattern) {
        List<Request> rs = TrafficGenerator.generate(pattern, 400, 300, 20, 2026L);
        SimConfig c = SimConfig.of(4, 20, 8);
        double dispatch = avgTotal(rs, c);
        assertTrue(dispatch < avgTotal(rs, c.withScheduler(SchedulerType.NEAREST_CAR)));
    }

    private double avgTotal(List<Request> rs, SimConfig c) {
        return Stats.summarize(service.run(rs, c).passengers()).totalTime().mean();
    }
}
