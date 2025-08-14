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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pre-production test that validates our implementation against curl.
 * This test requires:
 * 1. Valid PAGERDUTY_API_TOKEN environment variable
 * 2. curl installed on the system
 * 3. Network access to PagerDuty API
 */
@EnabledIfEnvironmentVariable(named = PagerDuty.TOKEN, matches = ".+")
class PagerDutyPreProductionTest {

    private static final Logger log = LoggerFactory.getLogger(PagerDutyPreProductionTest.class);

    private String apiToken;
    private PagerDutyUserService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        apiToken = System.getenv(PagerDuty.TOKEN);

        service = new PagerDutyUserService(apiToken);
        objectMapper = new ObjectMapper();

        log.info("Pre-production test setup complete");
    }

    @Test
    void testUsersListMatchesCurl() throws Exception {
        log.info("Running pre-production test against live API using curl...");

        // Test different pagination scenarios
        var testCases = List.of(
                new TestCase(0, 10, "First page with 10 items"),
                new TestCase(0, 1, "Single item per page"),
                new TestCase(0, 25, "Medium page size"),
                new TestCase(5, 10, "Middle offset with 10 items"),
                new TestCase(10, 5, "Different offset with small page")
        );

        for (var testCase : testCases) {
            log.info("Testing: {}", testCase.description);

            try {
                // 1. Get page using curl
                var curlUsers = getUsersViaCurl(testCase.offset, testCase.limit);
                log.info("Retrieved {} users via curl", curlUsers.size());

                // 2. Get page using our service
                var serviceResponse = service.getUsersPage(testCase.offset, testCase.limit);
                var serviceUsers = serviceResponse.data();
                log.info("Retrieved {} users via service", serviceUsers.size());

                // 3. Compare counts
                assertEquals(curlUsers.size(), serviceUsers.size(),
                        String.format("User count mismatch for %s", testCase.description));

                // 4. Compare each user
                for (int i = 0; i < curlUsers.size(); i++) {
                    var curlUser = curlUsers.get(i);
                    var serviceUser = serviceUsers.get(i);

                    assertEquals(curlUser.id(), serviceUser.id(),
                            String.format("ID mismatch at index %d for %s", i, testCase.description));
                    assertEquals(curlUser.name(), serviceUser.name(),
                            String.format("Name mismatch at index %d for %s", i, testCase.description));
                    assertEquals(curlUser.email(), serviceUser.email(),
                            String.format("Email mismatch at index %d for %s", i, testCase.description));
                    assertEquals(curlUser.type(), serviceUser.type(),
                            String.format("Type mismatch at index %d for %s", i, testCase.description));
                }

                log.info("✓ Test passed: {}", testCase.description);

                // Small delay between tests
                Thread.sleep(200);

            } catch (Exception e) {
                // Handle rate limits or invalid offsets gracefully
                if (e.getMessage() != null &&
                        (e.getMessage().contains("429") || e.getMessage().contains("400"))) {
                    log.warn("Skipping test case due to API limits: {}", testCase.description);
                } else {
                    throw e;
                }
            }
        }

        log.info("✓ All curl comparison tests completed!");
    }

    // Simple test case record
    private record TestCase(int offset, int limit, String description) {}

    @Test
    @SuppressWarnings("BusyWait") // Intentional delay for API rate limiting
    void testFullPaginationMatchesCurl() throws Exception {
        log.info("Running full pagination test (this may take a while)...");

        // 1. Get all users using curl with pagination
        var allCurlUsers = getAllUsersViaCurl();
        //noinspection LoggingSimilarMessage
        log.info("Retrieved {} total users via curl", allCurlUsers.size());

        // 2. Get all users using our service
        var allServiceUsers = new ArrayList<PagerDutyUser>();
        var offset = 0;
        var limit = 25;

        while (true) {
            var page = service.getUsersPage(offset, limit);
            allServiceUsers.addAll(page.data());

            if (!page.hasMorePages()) {
                break;
            }
            offset = page.nextOffset();

            // Add small delay to respect rate limits
            Thread.sleep(100);
        }

        //noinspection LoggingSimilarMessage
        log.info("Retrieved {} total users via service", allServiceUsers.size());

        // 3. Compare totals
        assertEquals(allCurlUsers.size(), allServiceUsers.size(),
                "Total user count mismatch");

        // 4. Verify all user IDs match
        var curlIds = allCurlUsers.stream().map(PagerDutyUser::id).collect(Collectors.toSet());
        var serviceIds = allServiceUsers.stream().map(PagerDutyUser::id).collect(Collectors.toSet());

        assertEquals(curlIds, serviceIds, "User ID sets don't match");

        log.info("✓ Full pagination matches between curl and service");
    }

    private List<PagerDutyUser> getUsersViaCurl(int offset, int limit) throws Exception {
        var url = String.format("https://api.pagerduty.com/users?offset=%d&limit=%d", offset, limit);
        var curlCommand = new String[] {
                "curl", "-s", "-X", "GET",
                "-H", "Authorization: Token token=" + apiToken,
                "-H", "Accept: application/json",
                url
        };

        log.debug("Executing curl command for offset={}, limit={}", offset, limit);

        var process = new ProcessBuilder(curlCommand).start();
        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        var response = reader.lines().collect(Collectors.joining("\n"));

        var exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("curl failed with exit code: " + exitCode);
        }

        var usersResponse = objectMapper.readValue(response, PagerDutyUsersResponse.class);
        return usersResponse.users();
    }

    @SuppressWarnings("BusyWait") // Intentional delay for API rate limiting
    private List<PagerDutyUser> getAllUsersViaCurl() throws Exception {
        var allUsers = new ArrayList<PagerDutyUser>();
        var offset = 0;
        var limit = 100; // Max for PagerDuty

        while (true) {
            var url = String.format("https://api.pagerduty.com/users?offset=%d&limit=%d", offset, limit);
            var curlCommand = new String[] {
                    "curl", "-s", "-X", "GET",
                    "-H", "Authorization: Token token=" + apiToken,
                    "-H", "Accept: application/json",
                    url
            };

            var process = new ProcessBuilder(curlCommand).start();
            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            var response = reader.lines().collect(Collectors.joining("\n"));

            var exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("curl failed with exit code: " + exitCode);
            }

            var usersResponse = objectMapper.readValue(response, PagerDutyUsersResponse.class);
            allUsers.addAll(usersResponse.users());

            log.debug("Fetched page at offset={}, got {} users, more={}",
                    offset, usersResponse.userCount(), usersResponse.hasMorePages());

            if (!usersResponse.hasMorePages()) {
                break;
            }

            offset = usersResponse.nextOffset();
            Thread.sleep(100); // Respect rate limits
        }

        return allUsers;
    }
}