package me.riddle.fintech.application.service.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

/**
 * A stub for an actual remote user service test
 */
class PagerDutyUserServiceTest {

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private HttpClient httpClient;

    @BeforeEach
    void setUpProfiledHttpClient() {
        httpClient = HttpClient
                .newBuilder()
                .build();

    }

    @Test
    void testRetrievingRemoteUser() {

    }

}