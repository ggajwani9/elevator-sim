package com.elevatorsim.service;

import com.elevatorsim.config.SimConfig;
import com.elevatorsim.model.Elevator;
import com.elevatorsim.model.Passenger;

import java.util.List;

/**
 * Picks a car for a new passenger, once, at the tick they ask.
 * It sees only that passenger and the cars, never future requests.
 */
@FunctionalInterface
public interface Scheduler {

    /** @return the id of the chosen car */
    int choose(Passenger passenger, List<Elevator> cars, int tick);

    static Scheduler create(SimConfig config) {
        return switch (config.scheduler()) {
            case DISPATCH -> Scheduler::dispatch;
            case NEAREST_CAR -> Scheduler::nearestCar;
        };
    }

    /**
     * Destination dispatch. For each car: how much does everyone's total time go up if this
     * car takes the passenger? (Their own trip plus any delay to people the car already has.)
     * The cheapest car wins. On a tie, prefer a car that's already busy, so idle cars stay free.
     */
    static int dispatch(Passenger p, List<Elevator> cars, int tick) {
        Elevator best = null;
        long bestCost = Long.MAX_VALUE;
        for (Elevator car : cars) {
            long cost = car.predictTotalTime(tick, p) - car.predictTotalTime(tick, null);
            boolean busyBeatsIdle = cost == bestCost && car.outstanding() > 0 && best.outstanding() == 0;
            if (cost < bestCost || busyBeatsIdle) {
                best = car;
                bestCost = cost;
            }
        }
        return best.id();
    }

    /** The car closest to the caller's floor; on a tie, the lower id. */
    static int nearestCar(Passenger p, List<Elevator> cars, int tick) {
        Elevator best = cars.get(0);
        for (Elevator car : cars) {
            if (Math.abs(car.floor() - p.source()) < Math.abs(best.floor() - p.source())) {
                best = car;
            }
        }
        return best.id();
    }
}
