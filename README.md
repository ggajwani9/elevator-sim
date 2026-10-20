# Elevator System Simulation

A discrete-time simulation of elevators run by a **destination dispatch** controller.
Java 17, Spring Boot 3, about 1,000 lines.

* Each passenger gives origin and destination up front and is assigned to a car the moment
  they ask. The assignment never changes.
* One tick = one floor of travel. The clock moves one tick at a time.
* The simulation never looks ahead in the request list.
* Everyone is served; capacity and direction rules are never broken.

## Run it

Needs JDK 17+ and Maven.

```bash
mvn verify                      # build + all tests
mvn package -DskipTests         # build target/elevator-sim.jar
```

**Command line** — writes `out/positions.csv` and `out/passengers.csv` and prints a summary:

```bash
java -jar target/elevator-sim.jar --input examples/walkthrough.csv --elevators 2 --floors 10 --capacity 4
java -jar target/elevator-sim.jar --input examples/brief.csv --elevators 2 --capacity 10

# random traffic, both schedulers side by side (--pattern random|morning_rush)
java -jar target/elevator-sim.jar --generate 400 --pattern random --duration 300 --floors 20 --seed 2026 --compare
```

Options: `--elevators` (default 4), `--floors` (default: highest floor in the input),
`--capacity` (8), `--scheduler dispatch|nearest_car`, `--compare`, `--out` (`out`).
Bad input exits with code 2 and says what was wrong.

**REST API** — run with no arguments (port 8080). Example calls are in `requests.http`:

* `POST /api/simulations` → summary, per-car stats, observations, passengers, positions
* `POST /api/simulations/compare` → the same requests through both schedulers

```json
{"elevators": 2, "floors": 10, "capacity": 4, "requests": [
  {"time": 0, "id": "Alice", "source": 1, "dest": 8}, ...]}
```

## Code layout

| Folder | What's in it |
|---|---|
| `config` | `SimConfig` (building settings, validated), `SchedulerType` |
| `model` | `Request`, `Passenger`, `SimResult`, and `Elevator` — a car and its rules |
| `service` | `SimulationService` (the clock), `RequestFeed` (no peeking), `Scheduler` (dispatch and nearest car) |
| `utils` | `CsvUtils` (read/write CSV), `Stats` (summary + observations), `TrafficGenerator` (random requests) |
| `controller` | REST endpoints and error handling |
| root | `ElevatorSimApplication` (starts the API) and `Cli` (command line) |

## How it works

**Every tick** (`SimulationService`):

1. **Release** requests made at this tick — never later ones.
2. **Assign** each to a car, immediately and for good.
3. **Log** every car's floor.
4. **Step** every car.

**A car's step** (`Elevator`):

1. Unload riders who have arrived.
2. Pick a direction:
   * riders aboard → keep going their way (never turn back with someone inside);
   * someone has waited longer than `maxWait` → go and get them;
   * more pickups ahead → keep going (LOOK);
   * otherwise → head for the nearest waiting passenger, or stay idle.
3. Board waiting passengers here who go the same way, oldest first, up to capacity.
   While someone is overdue, keep one seat free for them.
4. Move one floor.

**Choosing a car** (`Scheduler.dispatch`): for each car, play its future forward twice —
with and without the new passenger — using the car's own `step` code. The difference in
everyone's summed total time is the cost. Cheapest car wins. On a tie, prefer a car that's
already busy so idle cars stay free for calls nobody has made yet. Because the look-ahead
runs the real car code, the prediction is exact.

For comparison there is one simple baseline, **nearest car**: pick the car closest to the caller.

## Walkthrough

10 floors, 2 elevators, capacity 4, all at the lobby (`examples/walkthrough.csv`):

```
time,id,source,dest
0,Alice,1,8
0,Bob,1,5
3,Carol,7,2
4,Dan,6,9
```

| t | car 0 | car 1 | What happens |
|---|---|---|---|
| 0 | 1 | 1 | Alice and Bob call. Both go to car 0 and board. |
| 3 | 4 ↑ | 1 | Carol calls at 7, going down. Cost: car 0 = 10, car 1 = 11 → car 0. |
| 4 | 5 ↑ | 1 | Dan calls at 6, going up. Cost: car 0 = 6, car 1 = 8 → car 0. Bob gets off at 5. |
| 5 | 6 ↑ | 1 | Dan boards. |
| 6 | 7 ↑ | 1 | Car 0 passes Carol's floor without stopping: it's going up, she's going down. |
| 7 | 8 ↑ | 1 | Alice gets off. |
| 8 | 9 ↑ | 1 | Dan gets off. Nothing above, so the car turns. |
| 10 | 7 ↓ | 1 | Carol boards. |
| 15 | 2 ↓ | 1 | Carol gets off. Done. |

| Passenger | Wait | Ride | Total |
|---|---:|---:|---:|
| Alice | 0 | 7 | 7 |
| Bob | 0 | 4 | 4 |
| Carol | 7 | 5 | 12 |
| Dan | 1 | 3 | 4 |

Average wait 2.0, average total 6.75. `positions.csv` starts:

```
time,elevator_0,elevator_1
0,1,1
1,2,1
2,3,1
```

Things worth noticing:

* **t = 0, ties.** Alice costs 7 on either car; both are idle, so the lower id wins. Bob
  costs 4 on either car; car 0 is already going up, so it takes him and car 1 stays free.
* **t = 3, a close call.** Car 0 can finish going up, turn at 8 and reach Carol for a total
  of 10. Car 1 would need 6 floors to reach her: 11. Car 0 wins by one tick.
* **t = 4, a deliberate trade-off.** Dan pushes car 0's turn up a floor, costing Carol 2
  ticks. The dispatcher knows and takes it anyway: car 0 costs 6 in all (Dan 4 + Carol's
  2), car 1 would cost 8.
* **The limit.** Had the controller known Dan was coming, it would have sent Carol to car 1
  (11 instead of 12) and saved one tick overall. But assignments are fixed when someone
  calls and nobody can see future calls, so that tick can't be recovered. Real destination
  dispatch has the same limit.
* **t = 6, direction logic.** Car 0 is at Carol's floor but going the other way, so she
  waits for the return trip. That's why she has 7 of the 8 wait ticks.

All of this is checked in `SimulationServiceTest.walkthroughScenario`.

## Results

400 requests over 300 ticks, 4 elevators, 20 floors, capacity 8, seed 2026:

| Traffic | Scheduler | Avg wait | p95 wait | Max wait | Avg total |
|---|---|---:|---:|---:|---:|
| Random | **dispatch** | **5.9** | **14** | **36** | **12.6** |
| | nearest car | 14.7 | 36 | 57 | 21.3 |
| Morning rush | **dispatch** | **64.4** | **132** | **150** | **74.4** |
| | nearest car | 159.5 | 317 | 371 | 169.5 |

* Dispatch roughly halves the average wait and the worst wait in both cases.
* **Nearest car** looks only at distance, so it piles people onto whichever car happens to
  be close, while the others sit idle.
* **Morning rush** is hard for everyone: all trips start at the lobby, so the limit is how
  fast cars get back down. Parking idle cars at the lobby is the classic fix (not built).
* **Fairness vs efficiency:** dispatch minimises the *sum* of journey times, which can let
  one passenger wait a long time. `maxWait` caps that. `RulesTest.nobodyWaitsForever`
  builds a case where a passenger is never picked up without it (2,674 ticks) and is
  with it (208 ticks).

## Assumptions

* Floors 1..n, lobby = 1, all cars start there.
* Boarding takes no time (the brief measures time only in floors).
* wait = pickup − request, travel = drop-off − pickup, total = drop-off − request.
* Requests made in the same tick are assigned in input order.
* Invalid requests (same floor, outside the building, duplicate id, negative time) are
  rejected with a message naming the request, never silently dropped.
* The run ends at the last drop-off.

## Tests

`mvn verify` runs about 70 tests:

* the brief's sample and the walkthrough, checked tick by tick;
* capacity, direction, one floor per tick and "everyone delivered", checked on every
  passenger and every tick for every scheduler × traffic pattern and random buildings;
* no peeking: the scheduler sees each request once, at its own tick;
* nobody waits forever (with and without `maxWait`);
* each scheduler's choice rule; dispatch beats both baselines;
* CSV, CLI exit codes, and the REST API.

## What I'd improve with more time

* Express elevators and zones.
* Door time, and parking idle cars at the lobby in the morning.
* Faster dispatch: update costs incrementally instead of replaying each car.
* Charts of wait times and car movement from the CSV output.

## Time spent

About _N_ hours. <!-- fill in -->
# elevator-sim
