package com.elevatorsim.service;

import com.elevatorsim.config.SimConfig;
import com.elevatorsim.model.Elevator;
import com.elevatorsim.model.Passenger;
import com.elevatorsim.model.Request;
import com.elevatorsim.model.SimResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs the clock. Every tick:
 * <ol>
 *   <li>release the requests made now (never later ones);</li>
 *   <li>assign each to a car, immediately and for good;</li>
 *   <li>log every car's floor;</li>
 *   <li>step every car.</li>
 * </ol>
 * Stops once every request has been released and delivered.
 */
@Service
public class SimulationService {

    public SimResult run(List<Request> requests, SimConfig config) {
        return run(requests, config, Scheduler.create(config));
    }

    public SimResult run(List<Request> requests, SimConfig config, Scheduler scheduler) {
        validate(requests, config);

        List<Elevator> cars = new ArrayList<>();
        for (int id = 0; id < config.elevators(); id++) {
            cars.add(new Elevator(id, config));
        }
        RequestFeed feed = new RequestFeed(requests);
        List<Passenger> passengers = new ArrayList<>();
        List<int[]> positions = new ArrayList<>();

        for (int tick = 0; ; tick++) {
            if (tick > config.maxTicks()) {
                throw new IllegalStateException("stopped after " + config.maxTicks() + " ticks; not delivered: "
                        + passengers.stream().filter(p -> !p.isDelivered()).map(Passenger::id).toList());
            }
            for (Request r : feed.release(tick)) {                       // 1. release
                Passenger p = new Passenger(r);
                cars.get(scheduler.choose(p, cars, tick)).assign(p);     // 2. assign
                passengers.add(p);
            }
            int[] row = new int[cars.size()];                             // 3. log
            for (Elevator car : cars) {
                row[car.id()] = car.floor();
            }
            positions.add(row);
            for (Elevator car : cars) {                                   // 4. step
                car.step(tick);
            }
            boolean allDelivered = passengers.stream().allMatch(Passenger::isDelivered);
            if (allDelivered && !feed.hasPending()) {
                break;                                                    // last row = last drop-off
            }
        }

        List<String> inputOrder = requests.stream().map(Request::id).toList();
        passengers.sort(Comparator.comparingInt(p -> inputOrder.indexOf(p.id())));
        return new SimResult(config, passengers, positions);
    }

    /** Rejects impossible requests up front instead of silently dropping them. */
    private static void validate(List<Request> requests, SimConfig config) {
        if (requests == null) {
            throw new InvalidRequestException("requests are required");
        }
        Set<String> ids = new HashSet<>();
        for (Request r : requests) {
            if (r == null || r.id() == null || r.id().isBlank()) {
                throw new InvalidRequestException("every request needs an id: " + r);
            }
            String id = r.id();
            if (!ids.add(id)) {
                throw new InvalidRequestException(id + ": duplicate id");
            }
            if (r.time() < 0) {
                throw new InvalidRequestException(id + ": time must be 0 or more");
            }
            if (r.source() < 1 || r.source() > config.floors() || r.dest() < 1 || r.dest() > config.floors()) {
                throw new InvalidRequestException(id + ": floors must be between 1 and " + config.floors());
            }
            if (r.source() == r.dest()) {
                throw new InvalidRequestException(id + ": source and destination are the same");
            }
        }
    }
}
