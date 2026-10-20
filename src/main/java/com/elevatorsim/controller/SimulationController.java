package com.elevatorsim.controller;

import com.elevatorsim.config.SchedulerType;
import com.elevatorsim.config.SimConfig;
import com.elevatorsim.model.Request;
import com.elevatorsim.model.SimResult;
import com.elevatorsim.service.SimulationService;
import com.elevatorsim.utils.Stats;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** HTTP front door. Stateless: every call runs its own simulation. */
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    /** Body. Only {@code requests} is required: defaults are 4 cars, highest floor used, capacity 8, dispatch. */
    public record RunRequest(Integer elevators, Integer floors, Integer capacity,
                             SchedulerType scheduler, List<Request> requests) {

        SimConfig config() {
            if (requests == null) {
                throw new IllegalArgumentException("requests is required");
            }
            int highest = requests.stream().filter(r -> r != null)
                    .mapToInt(r -> Math.max(r.source(), r.dest())).max().orElse(2);
            return SimConfig.of(elevators == null ? 4 : elevators, floors == null ? Math.max(2, highest) : floors,
                    capacity == null ? 8 : capacity).withScheduler(scheduler == null ? SchedulerType.DISPATCH : scheduler);
        }
    }

    public record PassengerView(String id, int time, int source, int dest, int elevator,
                                int pickup, int dropoff, int waitTime, int travel, int total) {
    }

    public record RunResponse(int lastTick, Stats.Summary summary, List<Stats.CarStats> perCar,
                              List<String> observations, List<PassengerView> passengers, List<int[]> positions) {
    }

    public record CompareRow(SchedulerType scheduler, Stats.Summary summary) {
    }

    @PostMapping
    public RunResponse run(@RequestBody RunRequest body) {
        SimResult r = service.run(body.requests(), body.config());
        List<PassengerView> passengers = r.passengers().stream()
                .map(p -> new PassengerView(p.id(), p.requestTime(), p.source(), p.dest(), p.elevatorId(),
                        p.pickupTime(), p.dropoffTime(), p.waitTime(), p.travelTime(), p.totalTime()))
                .toList();
        return new RunResponse(r.lastTick(), Stats.summarize(r.passengers()), Stats.perCar(r),
                Stats.observations(r), passengers, r.positions());
    }

    /** Same requests through every scheduler. */
    @PostMapping("/compare")
    public List<CompareRow> compare(@RequestBody RunRequest body) {
        SimConfig config = body.config();
        return Arrays.stream(SchedulerType.values())
                .map(s -> new CompareRow(s, Stats.summarize(service.run(body.requests(), config.withScheduler(s)).passengers())))
                .toList();
    }
}
