package com.elevatorsim.utils;

import com.elevatorsim.model.Passenger;
import com.elevatorsim.model.Request;
import com.elevatorsim.model.SimResult;
import com.elevatorsim.service.InvalidRequestException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** Reads the requests file and writes the two output files. */
public final class CsvUtils {

    public static final String REQUEST_HEADER = "time,id,source,dest";
    public static final String PASSENGER_HEADER = "id,time,source,dest,elevator,pickup,dropoff,wait,travel,total";

    private CsvUtils() {
    }

    /** {@code time,id,source,dest} with a header row. Spaces and blank lines are fine. */
    public static List<Request> readRequests(Reader reader) throws IOException {
        BufferedReader in = new BufferedReader(reader);
        String header = in.readLine();
        if (header == null || !header.replace(" ", "").equalsIgnoreCase(REQUEST_HEADER)) {
            throw new InvalidRequestException("line 1: expected header " + REQUEST_HEADER);
        }
        List<Request> requests = new ArrayList<>();
        int lineNo = 1;
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            lineNo++;
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split(",", -1);
            try {
                if (f.length != 4) {
                    throw new NumberFormatException();
                }
                requests.add(new Request(Integer.parseInt(f[0].trim()), f[1].trim(),
                        Integer.parseInt(f[2].trim()), Integer.parseInt(f[3].trim())));
            } catch (NumberFormatException e) {
                throw new InvalidRequestException("line " + lineNo + ": expected time,id,source,dest with whole numbers");
            }
        }
        return requests;
    }

    /** One row per tick from 0: {@code time,elevator_0,elevator_1,...}. */
    public static void writePositions(SimResult r, Writer out) throws IOException {
        StringBuilder sb = new StringBuilder("time");
        for (int car = 0; car < r.config().elevators(); car++) {
            sb.append(",elevator_").append(car);
        }
        sb.append('\n');
        for (int t = 0; t < r.positions().size(); t++) {
            sb.append(t);
            for (int floor : r.positions().get(t)) {
                sb.append(',').append(floor);
            }
            sb.append('\n');
        }
        out.write(sb.toString());
        out.flush();
    }

    /** One row per passenger with every timing. */
    public static void writePassengers(SimResult r, Writer out) throws IOException {
        StringBuilder sb = new StringBuilder(PASSENGER_HEADER).append('\n');
        for (Passenger p : r.passengers()) {
            sb.append(p.id()).append(',').append(p.requestTime()).append(',').append(p.source()).append(',')
                    .append(p.dest()).append(',').append(p.elevatorId()).append(',').append(p.pickupTime()).append(',')
                    .append(p.dropoffTime()).append(',').append(p.waitTime()).append(',').append(p.travelTime())
                    .append(',').append(p.totalTime()).append('\n');
        }
        out.write(sb.toString());
        out.flush();
    }
}
