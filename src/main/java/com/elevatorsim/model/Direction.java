package com.elevatorsim.model;

public enum Direction {
    UP, DOWN, IDLE;

    public static Direction of(int from, int to) {
        return to > from ? UP : to < from ? DOWN : IDLE;
    }
}
