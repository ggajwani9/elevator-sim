package com.elevatorsim.model;

import com.elevatorsim.config.SimConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * One car. Every tick it does four things:
 * <ol>
 *   <li><b>Unload</b> riders who have arrived.</li>
 *   <li><b>Pick a direction</b>:
 *     <ul>
 *       <li>riders aboard: keep going their way (never turn back with someone inside);</li>
 *       <li>someone has waited longer than {@code maxWait}: go and get them;</li>
 *       <li>more pickups ahead: keep going (LOOK);</li>
 *       <li>otherwise: head for the nearest waiting passenger, or stay idle.</li>
 *     </ul></li>
 *   <li><b>Board</b> waiting passengers here who go the same way, oldest first, up to capacity.
 *       While someone is overdue, one seat is kept free for them, so the car can't fill up
 *       on the way to them. That is what guarantees nobody waits forever.</li>
 *   <li><b>Move</b> one floor.</li>
 * </ol>
 */
public final class Elevator {

    private final int id;
    private final int capacity;
    private final int maxWait;

    private int floor = 1;
    private Direction direction = Direction.IDLE;
    private final List<Passenger> waiting = new ArrayList<>();   // assigned, not yet picked up, oldest first
    private final List<Passenger> riders = new ArrayList<>();    // on board

    public Elevator(int id, SimConfig config) {
        this(id, config.capacity(), config.maxWait());
    }

    private Elevator(int id, int capacity, int maxWait) {
        this.id = id;
        this.capacity = capacity;
        this.maxWait = maxWait;
    }

    public int id() { return id; }
    public int floor() { return floor; }
    public Direction direction() { return direction; }
    public List<Passenger> waiting() { return List.copyOf(waiting); }
    public List<Passenger> riders() { return List.copyOf(riders); }

    /** Passengers this car still has to deliver. */
    public int outstanding() { return waiting.size() + riders.size(); }

    /** Gives a passenger to this car, for good. */
    public void assign(Passenger p) {
        p.assignTo(id);
        waiting.add(p);
    }

    /** One tick: unload, pick a direction, board, move. */
    public void step(int tick) {
        riders.removeIf(p -> {
            if (p.dest() != floor) {
                return false;
            }
            p.dropOff(tick);
            return true;
        });

        Passenger overdue = overdue(tick);
        direction = pickDirection(overdue);

        for (Passenger p : List.copyOf(waiting)) {
            if (riders.size() == capacity) {
                break;
            }
            int freeSeats = capacity - riders.size();
            boolean allowed = overdue == null || p == overdue || freeSeats > 1;  // keep a seat for the overdue one
            if (allowed && p.source() == floor && p.direction() == direction) {
                waiting.remove(p);
                riders.add(p);
                p.pickUp(tick);
            }
        }

        if (direction == Direction.UP) {
            floor++;
        } else if (direction == Direction.DOWN) {
            floor--;
        }
    }

    private Direction pickDirection(Passenger overdue) {
        if (!riders.isEmpty()) {
            return riders.get(0).direction();
        }
        if (waiting.isEmpty()) {
            return Direction.IDLE;
        }
        if (overdue != null) {
            return towards(overdue);
        }
        if (direction != Direction.IDLE && hasPickupAhead()) {
            return direction;
        }
        Passenger nearest = waiting.get(0);
        for (Passenger p : waiting) {
            if (Math.abs(p.source() - floor) < Math.abs(nearest.source() - floor)) {
                nearest = p;
            }
        }
        return towards(nearest);
    }

    /** The oldest waiting passenger, if they have waited longer than maxWait. */
    private Passenger overdue(int tick) {
        if (waiting.isEmpty() || tick - waiting.get(0).requestTime() <= maxWait) {
            return null;
        }
        return waiting.get(0);
    }

    /** The way to go to fetch {@code p}; once there, the way they want to go. */
    private Direction towards(Passenger p) {
        return p.source() == floor ? p.direction() : Direction.of(floor, p.source());
    }

    private boolean hasPickupAhead() {
        for (Passenger p : waiting) {
            if (direction == Direction.UP ? p.source() > floor : p.source() < floor) {
                return true;
            }
        }
        return false;
    }

    /**
     * Summed total time of everyone this car must deliver, plus {@code extra} if given,
     * assuming no more assignments. Plays a copy of this car forward with the same
     * {@link #step} code, so the answer is exactly what will happen. This car is not changed.
     */
    public long predictTotalTime(int tick, Passenger extra) {
        Elevator copy = new Elevator(id, capacity, maxWait);
        copy.floor = floor;
        copy.direction = direction;
        List<Passenger> everyone = new ArrayList<>();
        for (Passenger p : waiting) {
            copy.waiting.add(p.copy());
        }
        for (Passenger p : riders) {
            copy.riders.add(p.copy());
        }
        if (extra != null) {
            copy.waiting.add(extra.copy());
        }
        everyone.addAll(copy.waiting);
        everyone.addAll(copy.riders);

        for (int t = tick; copy.outstanding() > 0; t++) {
            if (t - tick > 100_000) {
                throw new IllegalStateException("car " + id + " look-ahead did not finish");
            }
            copy.step(t);
        }
        long sum = 0;
        for (Passenger p : everyone) {
            sum += p.totalTime();
        }
        return sum;
    }
}
