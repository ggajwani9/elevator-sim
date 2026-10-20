package com.elevatorsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliTest {

    @TempDir
    Path dir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return Cli.run(args, new PrintStream(out, true), new PrintStream(err, true));
    }

    @Test
    void writesBothFilesAndPrintsSummary() throws IOException {
        Path input = dir.resolve("walkthrough.csv");
        Files.writeString(input, "time,id,source,dest\n0,Alice,1,8\n0,Bob,1,5\n3,Carol,7,2\n4,Dan,6,9\n");

        int code = run("--input", input.toString(), "--elevators", "2", "--capacity", "4", "--out", dir.toString());

        assertEquals(Cli.OK, code, err.toString());
        List<String> positions = Files.readAllLines(dir.resolve("positions.csv"));
        assertEquals("time,elevator_0,elevator_1", positions.get(0));
        assertEquals(17, positions.size());
        assertEquals(5, Files.readAllLines(dir.resolve("passengers.csv")).size());
        assertTrue(out.toString().contains("6.75"), out.toString());
    }

    @Test
    void compareListsEveryScheduler() {
        assertEquals(Cli.OK, run("--generate", "100", "--floors", "15", "--compare"), err.toString());
        assertTrue(out.toString().contains("dispatch") && out.toString().contains("nearest_car"));
    }

    @Test
    void badInputExitsWithTwo() throws IOException {
        Path input = dir.resolve("bad.csv");
        Files.writeString(input, "time,id,source,dest\n0,ghost,1,99\n");
        assertEquals(Cli.BAD_INPUT, run("--input", input.toString(), "--floors", "20", "--out", dir.toString()));
        assertTrue(err.toString().contains("ghost"));
        assertEquals(Cli.BAD_INPUT, run("--input", dir.resolve("missing.csv").toString()));
        assertEquals(Cli.BAD_INPUT, run("--generate", "10", "--elevators", "11"));
        assertEquals(Cli.BAD_INPUT, run("--generate", "10", "--colour", "red"));
        assertEquals(Cli.BAD_INPUT, run());
    }

    @Test
    void onlyInputOrGenerateMeansCli() {
        assertTrue(Cli.isCliRun(new String[] {"--input", "x.csv"}));
        assertFalse(Cli.isCliRun(new String[] {"--server.port=9090"}));
    }
}
