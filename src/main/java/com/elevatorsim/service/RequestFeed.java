package com.elevatorsim.service;

import com.elevatorsim.model.Request;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hands out requests only when their time comes, so the simulation can never peek ahead.
 *
 * The requests are sorted by time once. {@code next} points at the first request not yet
 * handed out; each tick we hand out requests from there until we reach one that is in the future.
 */
public final class RequestFeed {

    private final List<Request> sorted;
    private int next = 0;
    private int lastTick = -1;

    public RequestFeed(List<Request> requests) {
        sorted = new ArrayList<>(requests);
        sorted.sort(Comparator.comparingInt(Request::time));   // same time: keeps input order
    }

    /** Returns every request with {@code time <= tick} that hasn't been handed out yet. */
    public List<Request> release(int tick) {
        if (tick < lastTick) {
            throw new IllegalStateException("time cannot go backwards: " + tick + " < " + lastTick);
        }
        lastTick = tick;
        List<Request> due = new ArrayList<>();
        while (next < sorted.size() && sorted.get(next).time() <= tick) {
            due.add(sorted.get(next));
            next++;
        }
        return due;
    }

    /** True while some request hasn't been handed out yet. */
    public boolean hasPending() {
        return next < sorted.size();
    }
}
