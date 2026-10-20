package com.elevatorsim;

import com.elevatorsim.model.Request;

import java.util.List;

/** Inputs shared by several tests. */
public final class TestData {

    private TestData() {
    }

    public static Request req(int time, String id, int source, int dest) {
        return new Request(time, id, source, dest);
    }

    /** The three rows from the brief. */
    public static List<Request> brief() {
        return List.of(req(0, "passenger1", 1, 51), req(0, "passenger2", 1, 37), req(10, "passenger3", 20, 1));
    }

    /** The tick-by-tick walkthrough (examples/walkthrough.csv): 10 floors, 2 elevators, capacity 4. */
    public static List<Request> walkthrough() {
        return List.of(req(0, "Alice", 1, 8), req(0, "Bob", 1, 5), req(3, "Carol", 7, 2), req(4, "Dan", 6, 9));
    }
}
