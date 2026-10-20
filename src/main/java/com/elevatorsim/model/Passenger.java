package com.elevatorsim.model;

/** A request plus what happened to it. A passenger is assigned to a car once, for good. */
public final class Passenger {

    private static final int NOT_YET = -1;

    private final Request request;
    private int elevatorId = NOT_YET;
    private int pickupTime = NOT_YET;
    private int dropoffTime = NOT_YET;

    public Passenger(Request request) {
        this.request = request;
    }

    public Request request() { return request; }
    public String id() { return request.id(); }
    public int requestTime() { return request.time(); }
    public int source() { return request.source(); }
    public int dest() { return request.dest(); }
    public Direction direction() { return request.direction(); }

    public int elevatorId() { return elevatorId; }
    public int pickupTime() { return pickupTime; }
    public int dropoffTime() { return dropoffTime; }
    public boolean isAssigned() { return elevatorId != NOT_YET; }
    public boolean isDelivered() { return dropoffTime != NOT_YET; }

    public int waitTime() { return pickupTime - requestTime(); }
    public int travelTime() { return dropoffTime - pickupTime; }
    public int totalTime() { return dropoffTime - requestTime(); }

    void assignTo(int carId) {
        if (isAssigned()) {
            throw new IllegalStateException(id() + " is already assigned to car " + elevatorId);
        }
        elevatorId = carId;
    }

    void pickUp(int tick) { pickupTime = tick; }

    void dropOff(int tick) { dropoffTime = tick; }

    /** Independent copy, used when a car plays its future forward. */
    Passenger copy() {
        Passenger p = new Passenger(request);
        p.elevatorId = elevatorId;
        p.pickupTime = pickupTime;
        p.dropoffTime = dropoffTime;
        return p;
    }

    @Override
    public String toString() {
        return id() + "[" + source() + "->" + dest() + " t=" + requestTime() + " car=" + elevatorId
                + " pickup=" + pickupTime + " dropoff=" + dropoffTime + "]";
    }
}
