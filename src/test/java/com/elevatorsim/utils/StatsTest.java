package com.elevatorsim.utils;

import com.elevatorsim.config.SimConfig;
import com.elevatorsim.model.SimResult;
import com.elevatorsim.service.SimulationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.elevatorsim.TestData.brief;
import static com.elevatorsim.TestData.walkthrough;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsTest {

    private final SimulationService service = new SimulationService();

    @Test
    void briefSummary() {
        Stats.Summary s = Stats.summarize(service.run(brief(), SimConfig.of(1, 51, 10)).passengers());
        assertEquals(0, s.waitTime().min());
        assertEquals(71, s.waitTime().max());
        assertEquals(71.0 / 3, s.waitTime().mean(), 1e-9);
        assertEquals(36, s.totalTime().min());
        assertEquals(90, s.totalTime().max());
        assertEquals(176.0 / 3, s.totalTime().mean(), 1e-9);
    }

    @Test
    void walkthroughSummary() {
        Stats.Summary s = Stats.summarize(service.run(walkthrough(), SimConfig.of(2, 10, 4)).passengers());
        assertEquals(2.0, s.waitTime().mean(), 1e-9);
        assertEquals(6.75, s.totalTime().mean(), 1e-9);
    }

    @Test
    void nearestRankPercentile() {
        List<Integer> v = List.of(18, 0, 2, 16, 4, 6, 14, 8, 12, 10);
        assertEquals(8, Stats.percentile(v, 50));
        assertEquals(18, Stats.percentile(v, 95));
    }

    @Test
    void perCarAndObservations() {
        SimResult r = service.run(brief(), SimConfig.of(2, 51, 10));
        assertEquals(List.of(new Stats.CarStats(0, 2, 50), new Stats.CarStats(1, 1, 38)), Stats.perCar(r));
        assertTrue(Stats.observations(r).stream().anyMatch(n -> n.contains("passenger3")));
    }

    @Test
    void emptyRun() {
        assertEquals(0, Stats.summarize(List.of()).count());
    }
}
