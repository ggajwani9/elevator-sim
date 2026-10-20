package com.elevatorsim.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerTest {

    private static final String WALKTHROUGH = """
            {"elevators": 2, "floors": 10, "capacity": 4, "requests": [
              {"time": 0, "id": "Alice", "source": 1, "dest": 8},
              {"time": 0, "id": "Bob",   "source": 1, "dest": 5},
              {"time": 3, "id": "Carol", "source": 7, "dest": 2},
              {"time": 4, "id": "Dan",   "source": 6, "dest": 9}]}
            """;

    @Autowired
    MockMvc mvc;

    @Test
    void runReturnsSummaryPassengersAndPositions() throws Exception {
        mvc.perform(post("/api/simulations").contentType(MediaType.APPLICATION_JSON).content(WALKTHROUGH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastTick").value(15))
                .andExpect(jsonPath("$.summary.waitTime.mean").value(closeTo(2.0, 1e-9)))
                .andExpect(jsonPath("$.summary.totalTime.mean").value(closeTo(6.75, 1e-9)))
                .andExpect(jsonPath("$.passengers[2].id").value("Carol"))
                .andExpect(jsonPath("$.passengers[2].pickup").value(10))
                .andExpect(jsonPath("$.positions", hasSize(16)));
    }

    @Test
    void compareRunsEveryScheduler() throws Exception {
        mvc.perform(post("/api/simulations/compare").contentType(MediaType.APPLICATION_JSON).content(WALKTHROUGH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void badInputIs400() throws Exception {
        mvc.perform(post("/api/simulations").contentType(MediaType.APPLICATION_JSON)
                        .content(WALKTHROUGH.replace("\"dest\": 9", "\"dest\": 99")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Dan")));
        mvc.perform(post("/api/simulations").contentType(MediaType.APPLICATION_JSON)
                        .content(WALKTHROUGH.replace("\"elevators\": 2", "\"elevators\": 11")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/simulations").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest());
    }
}
