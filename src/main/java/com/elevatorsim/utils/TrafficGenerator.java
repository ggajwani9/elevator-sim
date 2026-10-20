package com.elevatorsim.utils;

import com.elevatorsim.model.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Makes random requests for demos and tests. The same seed always gives the same requests.
 *
 * <ul>
 *   <li>RANDOM: start and end on any floor.</li>
 *   <li>MORNING_RUSH: everyone starts at the lobby (floor 1).</li>
 * </ul>
 */
public final class TrafficGenerator {

    public enum Pattern { RANDOM, MORNING_RUSH }

    private TrafficGenerator() {
    }

    /** {@code count} requests at random ticks between 0 and {@code duration - 1}, in time order, ids p1, p2, ... */
    public static List<Request> generate(Pattern pattern, int count, int duration, int floors, long seed) {
        Random random = new Random(seed);

        // 1. Pick a random time for each request, then put the times in order.
        List<Integer> times = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            times.add(random.nextInt(duration));
        }
        Collections.sort(times);

        // 2. For each time, pick a start floor and a different end floor.
        List<Request> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int source = (pattern == Pattern.MORNING_RUSH) ? 1 : randomFloor(random, floors);
            int dest = randomFloor(random, floors);
            while (dest == source) {
                dest = randomFloor(random, floors);
            }
            requests.add(new Request(times.get(i), "p" + (i + 1), source, dest));
        }
        return requests;
    }

    /** A random floor from 1 to floors. */
    private static int randomFloor(Random random, int floors) {
        return 1 + random.nextInt(floors);
    }
}
