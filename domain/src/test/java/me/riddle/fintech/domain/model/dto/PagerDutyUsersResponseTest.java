package me.riddle.fintech.domain.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagerDutyUsersResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testConstructorValidation() {
        // Negative limit
        assertThrows(IllegalArgumentException.class, () ->
                new PagerDutyUsersResponse(List.of(), -1, 0, false, null));

        // Negative offset
        assertThrows(IllegalArgumentException.class, () ->
                new PagerDutyUsersResponse(List.of(), 10, -1, false, null));
    }

    @Test
    void testDefensiveCopyOfUsers() {
        var user1 = new PagerDutyUser("id1", "User 1", "user1@test.com", "user");
        var user2 = new PagerDutyUser("id2", "User 2", "user2@test.com", "user");

        var mutableList = new ArrayList<PagerDutyUser>();
        mutableList.add(user1);
        mutableList.add(user2);

        var response = new PagerDutyUsersResponse(mutableList, 10, 0, false, 2);

        // Modify original list
        mutableList.add(new PagerDutyUser("id3", "User 3", "user3@test.com", "user"));

        // Response users should be unchanged
        assertEquals(2, response.userCount());
        assertEquals(List.of(user1, user2), response.users());
    }

    @Test
    void testNullUsersListBecomesEmpty() {
        var response = new PagerDutyUsersResponse(null, 10, 0, false, 0);

        assertNotNull(response.users());
        assertTrue(response.users().isEmpty());
        assertEquals(0, response.userCount());
        assertTrue(response.isEmpty());
    }

    @Test
    void testToPagedResponse() {
        var user1 = new PagerDutyUser("id1", "User 1", "user1@test.com", "user");
        var user2 = new PagerDutyUser("id2", "User 2", "user2@test.com", "user");

        var usersResponse = new PagerDutyUsersResponse(
                List.of(user1, user2), 25, 50, true, 100
        );

        var pagedResponse = usersResponse.toPagedResponse();

        assertEquals(25, pagedResponse.limit());
        assertEquals(50, pagedResponse.offset());
        assertTrue(pagedResponse.more());
        assertEquals(100, pagedResponse.total());
        assertEquals(List.of(user1, user2), pagedResponse.data());
    }

    @Test
    void testHasMorePages() {
        var hasMore = new PagerDutyUsersResponse(List.of(), 10, 0, true, null);
        assertTrue(hasMore.hasMorePages());

        var noMore = new PagerDutyUsersResponse(List.of(), 10, 0, false, null);
        assertFalse(noMore.hasMorePages());
    }

    @Test
    void testUserCountAndIsEmpty() {
        var empty = new PagerDutyUsersResponse(List.of(), 10, 0, false, 0);
        assertEquals(0, empty.userCount());
        assertTrue(empty.isEmpty());

        var user = new PagerDutyUser("id1", "User", "user@test.com", "user");
        var withUsers = new PagerDutyUsersResponse(List.of(user), 10, 0, false, 1);
        assertEquals(1, withUsers.userCount());
        assertFalse(withUsers.isEmpty());
    }

    @Test
    void testNextOffset() {
        var response = new PagerDutyUsersResponse(List.of(), 25, 75, true, 200);
        assertEquals(100, response.nextOffset());
    }

    @Test
    void testGetPaginationLoggingInfo() {
        var user = new PagerDutyUser("id1", "User", "user@test.com", "user");
        var response = new PagerDutyUsersResponse(List.of(user), 25, 50, true, 150);

        var logInfo = response.getPaginationLoggingInfo();

        assertEquals("offset=50, limit=25, count=1, more=true, total=150", logInfo);
    }

    @Test
    void testGetPaginationLoggingInfoWithNullTotal() {
        var response = new PagerDutyUsersResponse(List.of(), 10, 0, false, null);

        var logInfo = response.getPaginationLoggingInfo();

        assertEquals("offset=0, limit=10, count=0, more=false, total=null", logInfo);
    }

    @Test
    void testJacksonDeserialization() throws Exception {
        String json = """
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
                        "type": "user_reference",
                        "summary": "Jane Smith"
                    }
                ],
                "limit": 25,
                "offset": 0,
                "more": true,
                "total": 42
            }
            """;

        var response = objectMapper.readValue(json, PagerDutyUsersResponse.class);

        assertEquals(2, response.userCount());
        assertEquals(25, response.limit());
        assertEquals(0, response.offset());
        assertTrue(response.more());
        assertEquals(42, response.total());

        // Check first user
        var firstUser = response.users().getFirst();
        assertEquals("P123456", firstUser.id());
        assertEquals("John Doe", firstUser.name());
        assertEquals("john@example.com", firstUser.email());

        // Check second user
        var secondUser = response.users().get(1);
        assertEquals("P789012", secondUser.id());
        assertEquals("user_reference", secondUser.type());
        assertEquals("Jane Smith", secondUser.summary());
    }
}