package me.riddle.fintech.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PagingUserCanaryTest {

    @Test
    void testCanaryCreation() {
        PagingUserCanary canary = new PagingUserCanary("test-token");
        assertNotNull(canary);
    }
}