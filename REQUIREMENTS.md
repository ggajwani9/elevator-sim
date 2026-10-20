# Requirements

Source: *Elevator System Simulation – Take-Home Project*. Built in Java 17 / Spring Boot 3
(the brief says Python; a Java submission was agreed).

Each requirement has an ID and the test that proves it.

## From the brief

| ID | Requirement | How it is met | Test |
|---|---|---|---|
| R1 | Configurable elevators (1–10), floors, capacity | `config.SimConfig`, validated on construction | `SimConfigTest` |
| R2 | Destination dispatch: origin and destination known at request time; assigned immediately; assignment and destination final | Scheduler is called at the request tick; a passenger can't be assigned twice; `Request` is immutable | `RulesTest.noPeeking`, `ElevatorTest` |
| R3 | Discrete time: 1 tick = 1 floor; tick one unit at a time even across gaps | `SimulationService.run` loop; cars move at most one floor per tick | `SimulationServiceTest.gaps`, `Invariants` |
| R4 | A function that accepts a list of `time,id,source,dest` requests | `SimulationService.run(List<Request>, SimConfig)`; `CsvUtils.readRequests` | `SimulationServiceTest`, `CsvUtilsTest` |
| R5 | Do not peek ahead | `RequestFeed.release(t)` is the only way to see a request | `RequestFeedTest`, `RulesTest.noPeeking` |
| R6 | Serve every request eventually | LOOK car rules + `maxWait` (fetch the overdue passenger, keep a seat for them); the run only ends when everyone is delivered | `RulesTest.nobodyWaitsForever`, `Invariants` |
| R7 | Minimise total time (wait + travel) | `Scheduler.dispatch` picks the car that adds the least summed total time | `SchedulerTest` |
| R8 | Honour capacity and direction logic | Boarding stops at capacity; no reversal with a rider aboard | `ElevatorTest`, `Invariants` (every passenger, every tick) |
| R9 | Log every car's position at every tick from t = 0 | `positions.csv` (`time,elevator_0,...`) | `CsvUtilsTest`, `CliTest` |
| R10 | Min / max / average wait and total; notable observations | `utils.Stats`: `summarize`, `perCar`, `observations` | `StatsTest` |
| B1 | Bonus: other algorithms | `NEAREST_CAR` as a baseline; `--compare` | `SchedulerTest` |
| B3 | Bonus: fairness vs efficiency | `maxWait`; comparison results in the README | `RulesTest.nobodyWaitsForever` |

Not done (deliberately, to keep it simple): express elevators (B2), door/boarding time,
idle-car parking. See "What I'd improve" in the README.

A tick-by-tick worked example (Alice, Bob, Carol, Dan) is in the README and in
`SimulationServiceTest.walkthroughScenario`.

## Decisions on what the brief leaves open

| Question | Decision |
|---|---|
| Floor numbering | 1..n, lobby is 1 (the sample uses floor 1) |
| Where cars start | All at the lobby, idle |
| Boarding time | Zero: time is defined only in floors |
| wait / travel / total | wait = pickup − request; travel = drop-off − pickup; total = drop-off − request |
| Order inside a tick | release → assign → log positions → each car: unload, choose direction, board, move |
| Several requests in one tick | Assigned in input order |
| "Direction logic" | A car boards only passengers going its way and never reverses with someone aboard |
| Invalid input (same floor, outside the building, duplicate id, negative time) | Rejected up front with a message naming the request; never silently dropped |
| End of run | At the last drop-off |

## Worked examples (also tests)

Brief sample, 51 floors, capacity 10:

| | passenger1 1→51 | passenger2 1→37 | passenger3 20→1 @ t=10 | avg wait | avg total |
|---|---|---|---|---|---|
| 1 car | pickup 0, drop 50 | pickup 0, drop 36 | pickup 81, drop 100 (car must finish its up trip first) | 23.67 | 58.67 |
| 2 cars | car 0 | car 0 (sharing costs nothing; car 1 stays free) | car 1: pickup 29, drop 48 | 6.33 | 41.33 |
