package com.elevatorsim.model;

/** One input row: {@code time,id,source,dest}. Immutable, so a destination can't change. */
public record Request(int time, String id, int source, int dest) {

    public Direction direction() {
        return Direction.of(source, dest);
    }
}
