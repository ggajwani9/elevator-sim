package com.elevatorsim.service;

import com.elevatorsim.Invariants;
import com.elevatorsim.config.SchedulerType;
import com.elevatorsim.config.SimConfig;
import com.elevatorsim.model.Elevator;
import com.elevatorsim.model.Passenger;
import com.elevatorsim.model.Request;
import com.elevatorsim.model.SimResult;
import com.elevatorsim.utils.TrafficGenerator;
import com.elevatorsim.utils.TrafficGenerator.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static com.elevatorsim.TestData.req;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The brief's three objectives, checked across many runs. */
class RulesTest {

    private final SimulationService service = new SimulationService();

    static Stream<Arguments> schedulersAndPatterns() {
        List<Arguments> all = new ArrayList<>();
        for (SchedulerType s : SchedulerType.values()) {
            for (Pattern p : Pattern.values()) {
                all.add(Arguments.of(s, p));
            }
        }
        return all.stream();
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("schedulersAndPatterns")
    @DisplayName("capacity, direction and 'serve everyone' hold for every scheduler and traffic shape")
    void invariants(SchedulerType scheduler, Pattern pattern) {
        List<Request> rs = TrafficGenerator.generate(pattern, 250, 300, 20, 11L);
        Invariants.check(service.run(rs, SimConfig.of(4, 20, 6).withScheduler(scheduler)));
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8})
    @DisplayName("... and for random buildings")
    void randomBuildings(long seed) {
        Random rnd = new Random(seed);
        int floors = 2 + rnd.nextInt(39);
        SimConfig c = SimConfig.of(1 + rnd.nextInt(6), floors, 1 + rnd.nextInt(8))
                .withScheduler(rnd.nextBoolean() ? SchedulerType.DISPATCH : SchedulerType.NEAREST_CAR)
                .withMaxWait(5 + rnd.nextInt(4 * floors));
        List<Request> rs = TrafficGenerator.generate(rnd.nextBoolean() ? Pattern.RANDOM : Pattern.MORNING_RUSH,
                40 + rnd.nextInt(160), 50 + rnd.nextInt(300), floors, seed);
        Invariants.check(service.run(rs, c));
    }

    @ParameterizedTest
    @EnumSource(SchedulerType.class)
    @DisplayName("no peeking: each request reaches the scheduler exactly once, at its own tick")
    void noPeeking(SchedulerType type) {
        SimConfig c = SimConfig.of(3, 15, 4).withScheduler(type);
        List<Request> rs = TrafficGenerator.generate(Pattern.RANDOM, 120, 300, 15, 42L);
        Scheduler real = Scheduler.create(c);
        List<String> seen = new ArrayList<>();
        Scheduler spy = (Passenger p, List<Elevator> cars, int tick) -> {
            assertEquals(p.requestTime(), tick, p.id() + " shown at the wrong tick");
            seen.add(p.id());
            return real.choose(p, cars, tick);
        };
        service.run(rs, c, spy);
        assertEquals(rs.size(), seen.size());
    }

    /**
     * One car, capacity 2, kept busy by two streams (lobby -> 10 and 10 -> lobby) faster than it
     * can carry them. It is always full going up past floor 5 and always heading down when it
     * reaches 5 empty, so a passenger at 5 going up is left behind until the streams stop.
     */
    private static List<Request> starvationTraffic() {
        List<Request> rs = new ArrayList<>();
        for (int t = 0; t < 600; t += 2) {
            rs.add(req(t, "up" + t, 1, 10));
            rs.add(req(t + 1, "down" + t, 10, 1));
        }
        rs.add(req(30, "victim", 5, 10));
        return rs;
    }

    @Test
    @DisplayName("serve everyone: without maxWait one passenger waits over 1,500 ticks; with it, under 300")
    void nobodyWaitsForever() {
        SimConfig oneCar = SimConfig.of(1, 10, 2);
        SimResult without = service.run(starvationTraffic(), oneCar.withMaxWait(Integer.MAX_VALUE));
        SimResult with = service.run(starvationTraffic(), oneCar.withMaxWait(40));

        assertTrue(without.passenger("victim").waitTime() > 1_500, "without: " + without.passenger("victim"));
        assertTrue(with.passenger("victim").waitTime() < 300, "with: " + with.passenger("victim"));
        Invariants.check(with);
    }
}
