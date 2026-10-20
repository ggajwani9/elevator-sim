package com.elevatorsim.config;

/** How a car is picked for a new passenger. */
public enum SchedulerType {
    /** The car that adds the least total time for everyone. Default. */
    DISPATCH,
    /** The car closest to the caller. Simple baseline to compare against. */
    NEAREST_CAR
}
