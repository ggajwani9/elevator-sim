package com.elevatorsim.config;

/**
 * Building settings, checked when created.
 *
 * @param elevators         number of cars, 1 to 10
 * @param floors            floors 1..floors; floor 1 is the lobby
 * @param capacity          passengers per car
 * @param scheduler         how cars are chosen
 * @param maxWait           a passenger who has waited longer than this is fetched first
 * @param maxTicks          safety stop for the clock
 */
public record SimConfig(int elevators, int floors, int capacity, SchedulerType scheduler,
                        int maxWait, int maxTicks) {

    public SimConfig {
        check(elevators >= 1 && elevators <= 10, "elevators must be between 1 and 10, was " + elevators);
        check(floors >= 2, "floors must be at least 2, was " + floors);
        check(capacity >= 1, "capacity must be at least 1, was " + capacity);
        check(scheduler != null, "scheduler is required");
        check(maxWait >= 1, "maxWait must be at least 1, was " + maxWait);
        check(maxTicks >= 1, "maxTicks must be at least 1, was " + maxTicks);
    }

    /** Dispatch scheduler, maxWait = 4 x floors, maxTicks = 1,000,000. */
    public static SimConfig of(int elevators, int floors, int capacity) {
        return new SimConfig(elevators, floors, capacity, SchedulerType.DISPATCH, 4 * floors, 1_000_000);
    }

    public SimConfig withScheduler(SchedulerType s) {
        return new SimConfig(elevators, floors, capacity, s, maxWait, maxTicks);
    }

    public SimConfig withMaxWait(int w) {
        return new SimConfig(elevators, floors, capacity, scheduler, w, maxTicks);
    }

    public SimConfig withMaxTicks(int t) {
        return new SimConfig(elevators, floors, capacity, scheduler, maxWait, t);
    }

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new IllegalArgumentException(message);
        }
    }
}
