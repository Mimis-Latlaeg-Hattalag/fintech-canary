package me.riddle.fintech.application.service.dto;

import me.riddle.fintech.domain.model.dto.PagedResponse;
import me.riddle.fintech.domain.model.dto.PagerDutyUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;

import static org.junit.jupiter.api.Assertions.*;

class PagerDutyUserServiceTest {

    private PagerDutyUserService service;
    private StubHttpClient stubHttpClient;

    @BeforeEach
    void setUp() {
        stubHttpClient = new StubHttpClient();
        service = new PagerDutyUserService("test-token", stubHttpClient);
    }

    @Test
    void testServiceCreation() {
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
    void testGetUserSuccess() throws IOException, InterruptedException {
        // Arrange
        String userJson = """
            {
                "user": {
                    "id": "P123456",
                    "type": "user",
                    "name": "John Doe",
                    "email": "john@example.com",
                    "time_zone": "America/New_York",
                    "role": "admin"
                }
            }
            """;
        stubHttpClient.setResponse(userJson, 200);

        // Act
        PagerDutyUser user = service.getUser("P123456");

        // Assert
        assertNotNull(user);
        assertEquals("P123456", user.id());
        assertEquals("John Doe", user.name());
        assertEquals("john@example.com", user.email());
        assertEquals("America/New_York", user.timeZone());
        assertEquals("admin", user.role());
    }

    @Test
    void testGetUserNotFound() {
        // Arrange
        stubHttpClient.setResponse("Not Found", 404);

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> service.getUser("invalid-id"));
        assertTrue(exception.getMessage().contains("404"));
    }

    @Test
    void testGetUsersPageSuccess() throws IOException, InterruptedException {
        // Arrange
        String usersJson = """
            {
                "users": [
                    {
                        "id": "P123456",
                        "type": "user",
                        "name": "John Doe",
                        "email": "john@example.com"
                    },
                    {
                        "id": "P789012",
                        "type": "user",
                        "name": "Jane Smith",
                        "email": "jane@example.com"
                    }
                ],
                "limit": 25,
                "offset": 0,
                "more": true,
                "total": 42
            }
            """;
        stubHttpClient.setResponse(usersJson, 200);

        // Act
        PagedResponse<PagerDutyUser> page = service.getUsersPage(0, 25);

        // Assert
        assertNotNull(page);
        assertEquals(2, page.itemCount());
        assertEquals(25, page.limit());
        assertEquals(0, page.offset());
        assertTrue(page.hasMorePages());
        assertEquals(42, page.total());

        // Check users
        var users = page.data();
        assertEquals("P123456", users.get(0).id());
        assertEquals("John Doe", users.get(0).name());
        assertEquals("P789012", users.get(1).id());
        assertEquals("Jane Smith", users.get(1).name());
    }

    @Test
    void testGetUsersPageWithEmptyResult() throws IOException, InterruptedException {
        // Arrange
        String emptyJson = """
            {
                "users": [],
                "limit": 10,
                "offset": 100,
                "more": false,
                "total": 95
            }
            """;
        stubHttpClient.setResponse(emptyJson, 200);

        // Act
        PagedResponse<PagerDutyUser> page = service.getUsersPage(100, 10);

        // Assert
        assertNotNull(page);
        assertTrue(page.isEmpty());
        assertFalse(page.hasMorePages());
    }

    @Test
    void testGetUsersPageUnauthorized() {
        // Arrange
        stubHttpClient.setResponse("Unauthorized", 401);

        // Act & Assert
        IOException exception = assertThrows(IOException.class,
                () -> service.getUsersPage(0, 25));
        assertTrue(exception.getMessage().contains("401"));
    }

    @Test
    void testGetUsersPageWithUnknownFields() throws IOException, InterruptedException {
        // Arrange - API returns new fields we don't know about
        String futureJson = """
            {
                "users": [
                    {
                        "id": "P123456",
                        "type": "user",
                        "name": "Future User",
                        "email": "future@example.com",
                        "new_field": "new_value",
                        "another_new_field": 123
                    }
                ],
                "limit": 10,
                "offset": 0,
                "more": false,
                "total": 1
            }
            """;
        stubHttpClient.setResponse(futureJson, 200);

        // Act
        PagedResponse<PagerDutyUser> page = service.getUsersPage(0, 10);

        // Assert
        var user = page.data().getFirst();
        assertTrue(user.hasUnknownFields());
        assertEquals("new_value", user.getUnknownField("new_field"));
        assertEquals(123, user.getUnknownField("another_new_field"));
    }

    /**
     * Simple stub implementation of HttpClient for testing.
     */
    private static class StubHttpClient extends HttpClient {
        private String responseBody = "";
        private int statusCode = 200;

        void setResponse(String body, int code) {
            this.responseBody = body;
            this.statusCode = code;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler) {

            return (HttpResponse<T>) new StubHttpResponse(responseBody, statusCode);
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, BodyHandler<T> responseBodyHandler) {
            // Return a completed future with our stub response -- exactly as is
            //noinspection unchecked
            return java.util.concurrent.CompletableFuture.completedFuture(
                    (HttpResponse<T>) new StubHttpResponse(responseBody, statusCode));
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            // Return a completed future with our stub response (ignoring push promises) - exactly as is
            //noinspection unchecked
            return java.util.concurrent.CompletableFuture.completedFuture(
                    (HttpResponse<T>) new StubHttpResponse(responseBody, statusCode));
        }

        @Override
        public java.util.Optional<java.time.Duration> connectTimeout() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<java.net.Authenticator> authenticator() {
            return java.util.Optional.empty();
        }

        @Override
        public javax.net.ssl.SSLContext sslContext() {
            return null;
        }

        @Override
        public javax.net.ssl.SSLParameters sslParameters() {
            return null;
        }

        @Override
        public java.util.Optional<java.net.ProxySelector> proxy() {
            return java.util.Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public java.util.Optional<java.util.concurrent.Executor> executor() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<java.net.CookieHandler> cookieHandler() {
            return java.util.Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NORMAL;
        }
    }

    /**
     * Stub HttpResponse implementation.
     * DO NOT make this immutable!
     */
    @SuppressWarnings("ClassCanBeRecord")
    private static class StubHttpResponse implements HttpResponse<String> {
        private final String body;
        private final int statusCode;

        StubHttpResponse(String body, int statusCode) {
            this.body = body;
            this.statusCode = statusCode;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public HttpRequest request() {
            return HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.pagerduty.com/test"))
                    .build();
        }

        @Override
        public java.util.Optional<HttpResponse<String>> previousResponse() {
            return java.util.Optional.empty();
        }

        @Override
        public java.net.http.HttpHeaders headers() {
            return java.net.http.HttpHeaders.of(java.util.Map.of(), (k, v) -> true);
        }

        @Override
        public java.util.Optional<javax.net.ssl.SSLSession> sslSession() {
            return java.util.Optional.empty();
        }

        @Override
        public java.net.URI uri() {
            return java.net.URI.create("https://api.pagerduty.com/test");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}