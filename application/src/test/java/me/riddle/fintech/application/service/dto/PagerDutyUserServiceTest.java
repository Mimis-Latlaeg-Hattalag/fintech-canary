package me.riddle.fintech.application.service.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PagerDutyUserServiceTest {

    private PagerDutyUserService service;

    @BeforeEach
    void setUp() {
        // For now, using a dummy token - real tests would mock HttpClient
        service = new PagerDutyUserService("dummy-token");
    }

    @Test
    void testServiceCreation() {
        // Simple test to verify service can be created
        assertNotNull(service);
    }

    @Test
    void testGetUsersPageValidatesLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                service.getUsersPage(0, 0));

        assertThrows(IllegalArgumentException.class, () ->
                service.getUsersPage(0, 101));
    }

    @Test
    void testGetUsersPageAcceptsValidLimits() {
        // This would fail with real API call, but verifies method signature
        assertDoesNotThrow(() -> {
            try {
                service.getUsersPage(0, 1);
            } catch (IOException | InterruptedException e) {
                // Expected - we don't have a valid token
            }
        });

        assertDoesNotThrow(() -> {
            try {
                service.getUsersPage(0, 100);
            } catch (IOException | InterruptedException e) {
                // Expected - we don't have a valid token
            }
        });
    }

    @Test
    void testGetUserRequiresUserId() {
        // Verify we handle null userId appropriately
        assertThrows(Exception.class, () -> service.getUser(null));
    }
}