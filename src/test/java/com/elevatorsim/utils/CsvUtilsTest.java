package com.elevatorsim.utils;

import com.elevatorsim.config.SimConfig;
import com.elevatorsim.model.SimResult;
import com.elevatorsim.service.InvalidRequestException;
import com.elevatorsim.service.SimulationService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static com.elevatorsim.TestData.brief;
import static com.elevatorsim.TestData.req;
import static com.elevatorsim.TestData.walkthrough;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvUtilsTest {

    @Test
    void readsTheBriefFormat() throws IOException {
        String csv = "time,id,source,dest\n0,passenger1,1,51\n0,passenger2,1,37\n10,passenger3,20,1\n";
        assertEquals(brief(), CsvUtils.readRequests(new StringReader(csv)));
    }

    @Test
    void toleratesSpacesBlankLinesAndCase() throws IOException {
        assertEquals(List.of(req(0, "a", 1, 5)),
                CsvUtils.readRequests(new StringReader("TIME, id, Source, DEST\r\n\r\n 0 , a , 1 , 5 \r\n")));
    }

    @Test
    void namesTheBadLine() {
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> CsvUtils.readRequests(new StringReader("time,id,source,dest\n0,a,1,5\n0,b,one,5\n")));
        assertTrue(e.getMessage().startsWith("line 3"), e.getMessage());
        assertThrows(InvalidRequestException.class, () -> CsvUtils.readRequests(new StringReader("0,a,1,5\n")));
    }

    @Test
    void writesOneRowPerTick() throws IOException {
        SimResult r = new SimulationService().run(walkthrough(), SimConfig.of(2, 10, 4));
        StringWriter out = new StringWriter();
        CsvUtils.writePositions(r, out);
        assertTrue(out.toString().startsWith("time,elevator_0,elevator_1\n0,1,1\n1,2,1\n2,3,1\n"), out.toString());
        assertEquals(17, out.toString().lines().count());   // header + ticks 0..15
    }

    @Test
    void writesPassengerTimings() throws IOException {
        SimResult r = new SimulationService().run(walkthrough(), SimConfig.of(2, 10, 4));
        StringWriter out = new StringWriter();
        CsvUtils.writePassengers(r, out);
        List<String> lines = out.toString().lines().toList();
        assertEquals(CsvUtils.PASSENGER_HEADER, lines.get(0));
        assertEquals("Carol,3,7,2,0,10,15,7,5,12", lines.get(3));
    }
}
