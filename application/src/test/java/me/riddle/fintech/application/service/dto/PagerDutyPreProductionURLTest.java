package me.riddle.fintech.application.service.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.riddle.fintech.domain.model.dto.PagerDutyUser;
import me.riddle.fintech.domain.model.dto.PagerDutyUsersResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Alternative pre-production test using java.net.URL instead of curl.
 * This test runs when PAGERDUTY_API_TOKEN is set.
 */
@EnabledIfEnvironmentVariable(named = PagerDuty.TOKEN, matches = ".+")
class PagerDutyPreProductionURLTest {

    private static final Logger log = LoggerFactory.getLogger(PagerDutyPreProductionURLTest.class);

    private String apiToken;
    private PagerDutyUserService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        apiToken = System.getenv(PagerDuty.TOKEN);

        service = new PagerDutyUserService(apiToken);
        objectMapper = new ObjectMapper();

        log.info("Pre-production test setup complete with token: {}...",
                apiToken.substring(0, 4));
    }

    @Test
    void testUsersListMatchesURLConnection() throws Exception {
        log.info("Running pre-production test using URL connection...");

        // Test different pagination scenarios
        var testCases = List.of(
                new TestCase(0, 10, "First page with 10 items"),
                new TestCase(0, 1, "Single item per page"),
                new TestCase(0, 25, "Medium page size"),
                new TestCase(0, 100, "Maximum page size"),
                new TestCase(5, 10, "Middle offset with 10 items"),
                new TestCase(10, 5, "Different offset with small page"),
                new TestCase(20, 15, "Higher offset with medium page")
        );

        for (var testCase : testCases) {
            log.info("Testing: {}", testCase.description);

            try {
                // 1. Get page using URL
                var urlUsers = getUsersViaURL(testCase.offset, testCase.limit);
                log.info("Retrieved {} users via URL", urlUsers.size());

                // 2. Get page using our service
                var serviceResponse = service.getUsersPage(testCase.offset, testCase.limit);
                var serviceUsers = serviceResponse.data();
                log.info("Retrieved {} users via service", serviceUsers.size());

                // 3. Compare
                assertEquals(urlUsers.size(), serviceUsers.size(),
                        String.format("User count mismatch for %s", testCase.description));

                for (int i = 0; i < urlUsers.size(); i++) {
                    var urlUser = urlUsers.get(i);
                    var serviceUser = serviceUsers.get(i);

                    assertEquals(urlUser.id(), serviceUser.id(),
                            String.format("ID mismatch at index %d for %s", i, testCase.description));
                    assertEquals(urlUser.name(), serviceUser.name(),
                            String.format("Name mismatch at index %d for %s", i, testCase.description));
                    assertEquals(urlUser.email(), serviceUser.email(),
                            String.format("Email mismatch at index %d for %s", i, testCase.description));
                }

                log.info("✓ Test passed: {}", testCase.description);

                // Small delay between tests to respect rate limits
                Thread.sleep(200);

            } catch (Exception e) {
                // If we hit rate limits or the offset is beyond available users, log and continue
                if (e.getMessage() != null &&
                        (e.getMessage().contains("429") || e.getMessage().contains("400"))) {
                    log.warn("Skipping test case due to API limits: {}", testCase.description);
                } else {
                    throw e;
                }
            }
        }

        log.info("✓ All test cases completed!");
    }

    // Simple test case record
    private record TestCase(int offset, int limit, String description) {}

    private List<PagerDutyUser> getUsersViaURL(int offset, int limit) throws Exception {
        var urlString = String.format(
                "https://api.pagerduty.com/users?offset=%d&limit=%d", offset, limit);
        log.debug("Fetching from URL: {}", urlString);

        var url = URI.create(urlString).toURL();
        var connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Token token=" + apiToken);
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);

        var responseCode = connection.getResponseCode();
        log.debug("Response code: {}", responseCode);

        if (responseCode != 200) {
            throw new RuntimeException("Failed with HTTP " + responseCode);
        }

        try (var reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            var response = reader.lines().collect(Collectors.joining("\n"));
            log.trace("Raw response: {}", response.substring(0, Math.min(200, response.length())));

            var usersResponse = objectMapper.readValue(response, PagerDutyUsersResponse.class);
            log.debug("Parsed {} users, has more: {}",
                    usersResponse.userCount(), usersResponse.hasMorePages());

            return usersResponse.users();
        }
    }
}