package com.elevatorsim.model;

import com.elevatorsim.config.SimConfig;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Outcome of one run.
 *
 * @param passengers every passenger, in input order
 * @param positions  {@code positions.get(t)[car]} = floor of that car at tick t, from t = 0
 */
public record SimResult(SimConfig config, List<Passenger> passengers, List<int[]> positions) {

    public int lastTick() {
        return positions.size() - 1;
    }

    public int floorAt(int tick, int car) {
        return positions.get(tick)[car];
    }

    public Passenger passenger(String id) {
        return passengers.stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("no passenger " + id));
    }
}
