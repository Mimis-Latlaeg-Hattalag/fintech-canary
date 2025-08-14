package me.riddle.fintech.application.service.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.riddle.fintech.domain.model.dto.PagedResponse;
import me.riddle.fintech.domain.model.dto.PagerDutyUser;
import me.riddle.fintech.domain.model.dto.PagerDutyUsersResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Simple service for retrieving PagerDuty users via their API.
 */
public class PagerDutyUserService {
    private static final String BASE_URL = "https://api.pagerduty.com";         // NiceToHave: Maybe push to config file in refactoring.
    private static final Duration TIMEOUT = Duration.ofSeconds(30);             // NiceToHave: Current default - does remote support HEAD requests for parameters?

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiToken;

    public PagerDutyUserService(String apiToken) {
        this(apiToken, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    // Package-private constructor for testing
    PagerDutyUserService(String apiToken, HttpClient httpClient) {
        this.apiToken = apiToken;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get a single user by ID.
     * FixMe: Validate with a proper test next iteration.
     */
    @SuppressWarnings("UnusedReturnValue")
    public PagerDutyUser getUser(String userId) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users/" + userId))
                .timeout(TIMEOUT)
                .header("Authorization", "Token token=" + apiToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get user: " + response.statusCode());
        }

        // PagerDuty wraps single user in a "user" field
        var rootNode = objectMapper.readTree(response.body());
        return objectMapper.treeToValue(rootNode.get("user"), PagerDutyUser.class);
    }

    /**
     * Get a page of users.
     * FixMe: valiadte with a proper test next iteration.
     */
    @SuppressWarnings("UnusedReturnValue")
    public PagedResponse<PagerDutyUser> getUsersPage(int offset, int limit)
            throws IOException, InterruptedException {

        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users?offset=" + offset + "&limit=" + limit))
                .timeout(TIMEOUT)
                .header("Authorization", "Token token=" + apiToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get users: " + response.statusCode());
        }

        var usersResponse = objectMapper.readValue(response.body(), PagerDutyUsersResponse.class);
        return usersResponse.toPagedResponse();
    }
}