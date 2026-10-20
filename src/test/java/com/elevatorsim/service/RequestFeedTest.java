package com.elevatorsim.service;

import com.elevatorsim.model.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.elevatorsim.TestData.req;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** No peeking: a request is only visible once its time comes. */
class RequestFeedTest {

    private static List<String> ids(List<Request> rs) {
        return rs.stream().map(Request::id).toList();
    }

    @Test
    @DisplayName("release(t) returns only due requests, once, in time then input order")
    void release() {
        RequestFeed feed = new RequestFeed(List.of(req(5, "late", 1, 2), req(0, "a", 1, 2), req(0, "b", 2, 1)));
        assertEquals(List.of("a", "b"), ids(feed.release(0)));
        assertEquals(List.of(), ids(feed.release(4)));
        assertEquals(List.of("late"), ids(feed.release(5)));
        assertFalse(feed.hasPending());
    }

    @Test
    @DisplayName("time cannot go backwards")
    void noRewind() {
        RequestFeed feed = new RequestFeed(List.of(req(0, "a", 1, 2)));
        feed.release(5);
        assertThrows(IllegalStateException.class, () -> feed.release(4));
    }
}
