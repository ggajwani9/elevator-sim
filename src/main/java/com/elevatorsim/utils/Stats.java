package com.elevatorsim.utils;

import com.elevatorsim.model.Passenger;
import com.elevatorsim.model.SimResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/** Summary statistics and plain-English observations. */
public final class Stats {

    private Stats() {
    }

    /** One metric over all passengers. Percentiles are nearest-rank. */
    public record Metric(int min, int max, double mean, int p50, int p95) {
    }

    public record Summary(int count, Metric waitTime, Metric travelTime, Metric totalTime) {
    }

    public record CarStats(int car, int passengers, int floorsTravelled) {
    }

    public static Summary summarize(List<Passenger> ps) {
        return new Summary(ps.size(), metric(ps, Passenger::waitTime),
                metric(ps, Passenger::travelTime), metric(ps, Passenger::totalTime));
    }

    private static Metric metric(List<Passenger> ps, ToIntFunction<Passenger> f) {
        if (ps.isEmpty()) {
            return new Metric(0, 0, 0, 0, 0);
        }
        List<Integer> v = ps.stream().map(f::applyAsInt).sorted().toList();
        double mean = v.stream().mapToInt(Integer::intValue).average().orElse(0);
        return new Metric(v.get(0), v.get(v.size() - 1), mean, percentile(v, 50), percentile(v, 95));
    }

    /** Nearest rank: the smallest value with at least q% of values at or below it. */
    public static int percentile(List<Integer> values, int q) {
        if (values.isEmpty() || q < 1 || q > 100) {
            throw new IllegalArgumentException("need values and 1 <= q <= 100");
        }
        List<Integer> sorted = values.stream().sorted().toList();
        return sorted.get((q * sorted.size() + 99) / 100 - 1);
    }

    public static List<CarStats> perCar(SimResult r) {
        List<CarStats> out = new ArrayList<>();
        for (int car = 0; car < r.config().elevators(); car++) {
            int c = car;
            int served = (int) r.passengers().stream().filter(p -> p.elevatorId() == c).count();
            int floors = 0;
            for (int t = 1; t < r.positions().size(); t++) {
                floors += Math.abs(r.floorAt(t, car) - r.floorAt(t - 1, car));
            }
            out.add(new CarStats(car, served, floors));
        }
        return out;
    }

    public static List<String> observations(SimResult r) {
        List<Passenger> ps = r.passengers();
        if (ps.isEmpty()) {
            return List.of("No requests.");
        }
        Summary s = summarize(ps);
        Passenger longest = ps.stream().max(Comparator.comparingInt(Passenger::waitTime)).orElseThrow();
        long noWait = ps.stream().filter(p -> p.waitTime() == 0).count();
        List<String> notes = new ArrayList<>();
        notes.add(String.format("%d of %d passengers were picked up with no wait.", noWait, ps.size()));
        notes.add(String.format("Median wait %d, 95th percentile %d, worst %d ticks.",
                s.waitTime().p50(), s.waitTime().p95(), s.waitTime().max()));
        notes.add(String.format("Longest wait: %s (%d -> %d) waited %d ticks.",
                longest.id(), longest.source(), longest.dest(), longest.waitTime()));
        notes.add("Every ride is direct (travel = floor distance), so all delay is waiting.");
        return notes;
    }
}
