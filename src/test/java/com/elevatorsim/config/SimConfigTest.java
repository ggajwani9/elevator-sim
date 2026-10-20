package com.elevatorsim.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimConfigTest {

    @Test
    void acceptsValidBuildings() {
        assertEquals(1, SimConfig.of(1, 2, 1).elevators());
        assertEquals(10, SimConfig.of(10, 100, 20).elevators());
    }

    @Test
    void rejectsInvalidBuildings() {
        assertThrows(IllegalArgumentException.class, () -> SimConfig.of(0, 10, 4));
        assertThrows(IllegalArgumentException.class, () -> SimConfig.of(11, 10, 4));
        assertThrows(IllegalArgumentException.class, () -> SimConfig.of(2, 1, 4));
        assertThrows(IllegalArgumentException.class, () -> SimConfig.of(2, 10, 0));
    }

    @Test
    void defaults() {
        SimConfig c = SimConfig.of(4, 30, 8);
        assertEquals(SchedulerType.DISPATCH, c.scheduler());
        assertEquals(120, c.maxWait());
    }
}
