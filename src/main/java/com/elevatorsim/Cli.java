package com.elevatorsim;

import com.elevatorsim.config.SchedulerType;
import com.elevatorsim.config.SimConfig;
import com.elevatorsim.model.Request;
import com.elevatorsim.model.SimResult;
import com.elevatorsim.service.InvalidRequestException;
import com.elevatorsim.service.SimulationService;
import com.elevatorsim.utils.CsvUtils;
import com.elevatorsim.utils.Stats;
import com.elevatorsim.utils.TrafficGenerator;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Command line. Plain Java so it starts instantly and is easy to test. Exit 0 = ok, 2 = bad input. */
public final class Cli {

    public static final int OK = 0;
    public static final int BAD_INPUT = 2;

    private static final String USAGE = """
            Usage:  --input FILE | --generate N [--pattern random|morning_rush] [--duration T] [--seed S]
                    [--elevators 4] [--floors N] [--capacity 8] [--scheduler dispatch|nearest_car]
                    [--compare] [--out out]
            """;
    private static final Set<String> OPTIONS = Set.of("input", "generate", "pattern", "duration", "seed",
            "elevators", "floors", "capacity", "scheduler", "compare", "out");

    private Cli() {
    }

    public static boolean isCliRun(String[] args) {
        for (String a : args) {
            if (a.startsWith("--input") || a.startsWith("--generate")) {
                return true;
            }
        }
        return false;
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            Map<String, String> opt = parse(args);
            List<Request> requests = requests(opt);
            int highest = requests.stream().mapToInt(r -> Math.max(r.source(), r.dest())).max().orElse(2);
            SimConfig config = SimConfig.of(number(opt, "elevators", 4), number(opt, "floors", Math.max(2, highest)),
                    number(opt, "capacity", 8)).withScheduler(scheduler(opt.getOrDefault("scheduler", "dispatch")));
            SimulationService service = new SimulationService();

            if (opt.containsKey("compare")) {
                out.printf("%-12s %9s %9s %9s %10s%n", "scheduler", "avg wait", "p95 wait", "max wait", "avg total");
                for (SchedulerType s : SchedulerType.values()) {
                    Stats.Summary sum = Stats.summarize(service.run(requests, config.withScheduler(s)).passengers());
                    out.printf("%-12s %9.2f %9d %9d %10.2f%n", s.name().toLowerCase(Locale.ROOT),
                            sum.waitTime().mean(), sum.waitTime().p95(), sum.waitTime().max(), sum.totalTime().mean());
                }
                return OK;
            }

            SimResult result = service.run(requests, config);
            Path dir = Path.of(opt.getOrDefault("out", "out"));
            Files.createDirectories(dir);
            try (Writer w = Files.newBufferedWriter(dir.resolve("positions.csv"))) {
                CsvUtils.writePositions(result, w);
            }
            try (Writer w = Files.newBufferedWriter(dir.resolve("passengers.csv"))) {
                CsvUtils.writePassengers(result, w);
            }
            print(result, out);
            out.println("Wrote " + dir.resolve("positions.csv") + " and " + dir.resolve("passengers.csv"));
            return OK;
        } catch (NoSuchFileException e) {
            err.println("File not found: " + e.getFile());
        } catch (InvalidRequestException | IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            err.print(USAGE);
        } catch (IOException e) {
            err.println("I/O error: " + e.getMessage());
        }
        return BAD_INPUT;
    }

    private static void print(SimResult r, PrintStream out) {
        SimConfig c = r.config();
        Stats.Summary s = Stats.summarize(r.passengers());
        out.printf("%s | %d elevators, %d floors, capacity %d | %d passengers, last tick %d%n%n",
                c.scheduler(), c.elevators(), c.floors(), c.capacity(), s.count(), r.lastTick());
        out.printf("%-7s %5s %5s %7s %5s %5s%n", "", "min", "max", "mean", "p50", "p95");
        for (var row : List.of(Map.entry("wait", s.waitTime()), Map.entry("travel", s.travelTime()),
                Map.entry("total", s.totalTime()))) {
            Stats.Metric m = row.getValue();
            out.printf("%-7s %5d %5d %7.2f %5d %5d%n", row.getKey(), m.min(), m.max(), m.mean(), m.p50(), m.p95());
        }
        out.println();
        for (Stats.CarStats cs : Stats.perCar(r)) {
            out.printf("elevator %d: %d passengers, %d floors travelled%n", cs.car(), cs.passengers(), cs.floorsTravelled());
        }
        out.println();
        Stats.observations(r).forEach(n -> out.println("- " + n));
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> opt = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--")) {
                throw new IllegalArgumentException("unexpected argument " + args[i]);
            }
            String[] kv = args[i].substring(2).split("=", 2);
            String key = kv[0];
            if (!OPTIONS.contains(key)) {
                throw new IllegalArgumentException("unknown option --" + key);
            }
            if (key.equals("compare")) {
                opt.put(key, "true");
            } else if (kv.length == 2) {
                opt.put(key, kv[1]);
            } else if (i + 1 < args.length) {
                opt.put(key, args[++i]);
            } else {
                throw new IllegalArgumentException("--" + key + " needs a value");
            }
        }
        if (opt.containsKey("input") == opt.containsKey("generate")) {
            throw new IllegalArgumentException("give either --input or --generate");
        }
        return opt;
    }

    private static List<Request> requests(Map<String, String> opt) throws IOException {
        if (opt.containsKey("input")) {
            try (Reader r = Files.newBufferedReader(Path.of(opt.get("input")))) {
                return CsvUtils.readRequests(r);
            }
        }
        TrafficGenerator.Pattern pattern = TrafficGenerator.Pattern.valueOf(
                opt.getOrDefault("pattern", "random").toUpperCase(Locale.ROOT));
        return TrafficGenerator.generate(pattern, number(opt, "generate", 0), number(opt, "duration", 600),
                number(opt, "floors", 20), number(opt, "seed", 1));
    }

    private static SchedulerType scheduler(String name) {
        try {
            return SchedulerType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown scheduler " + name);
        }
    }

    private static int number(Map<String, String> opt, String key, int dflt) {
        String v = opt.get(key);
        try {
            return v == null ? dflt : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--" + key + " must be a whole number, was " + v);
        }
    }
}
