package me.riddle.fintech.domain.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PagerDutyUserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testRequiredFieldsCannotBeNull() {
        assertThrows(NullPointerException.class, () ->
                new PagerDutyUser(null, "Name", "email@test.com", "user"));

        assertThrows(NullPointerException.class, () ->
                new PagerDutyUser("id123", "Name", "email@test.com", null));
    }

    @Test
    void testSimplifiedConstructor() {
        var user = new PagerDutyUser("id123", "John Doe", "john@example.com", "user");

        assertEquals("id123", user.id());
        assertEquals("John Doe", user.name());
        assertEquals("john@example.com", user.email());
        assertEquals("user", user.type());
        assertNull(user.summary());
        assertNull(user.self());
        assertTrue(user.unknownFields().isEmpty());
    }

    @Test
    void testUnknownFieldsDefensiveCopy() {
        var mutableMap = new HashMap<String, Object>();
        mutableMap.put("custom", "value");

        var user = new PagerDutyUser("id123", "Name", "email", null, "user",
                null, null, null, null, null, null, null, null, null, mutableMap);

        // Modify original map
        mutableMap.put("another", "field");

        // User's map should be unchanged
        assertEquals(1, user.unknownFields().size());
        assertEquals("value", user.unknownFields().get("custom"));
        assertFalse(user.unknownFields().containsKey("another"));
    }

    @Test
    void testWithUnknownFieldImmutability() {
        var original = new PagerDutyUser("id123", "Name", "email@test.com", "user");
        var modified = original.withUnknownField("newField", "newValue");

        // Original unchanged
        assertFalse(original.hasUnknownFields());
        assertTrue(original.unknownFields().isEmpty());

        // New instance has the field
        assertTrue(modified.hasUnknownFields());
        assertEquals("newValue", modified.getUnknownField("newField"));

        // Other fields preserved
        assertEquals(original.id(), modified.id());
        assertEquals(original.name(), modified.name());
    }

    @Test
    void testJacksonSerialization() throws Exception {
        var user = new PagerDutyUser("id123", "John Doe", "john@example.com",
                "Summary", "user", "https://api.pagerduty.com/users/id123",
                "https://app.pagerduty.com/users/id123", "https://avatar.com/john.jpg",
                "#FF0000", "admin", "Senior Engineer", Boolean.TRUE, "Software Engineer",
                "America/New_York", Map.of("custom", "field"));

        String json = objectMapper.writeValueAsString(user);

        assertTrue(json.contains("\"id\":\"id123\""));
        assertTrue(json.contains("\"html_url\":\"https://app.pagerduty.com/users/id123\""));
        assertTrue(json.contains("\"time_zone\":\"America/New_York\""));
        assertTrue(json.contains("\"invitation_sent\":true"));
        assertTrue(json.contains("\"custom\":\"field\""));
    }

    @Test
    void testJacksonDeserialization() throws Exception {
        String json = """
            {
                "id": "PXYZ789",
                "name": "Jane Smith",
                "email": "jane@example.com",
                "type": "user",
                "summary": "Jane Smith - Engineering Lead",
                "self": "https://api.pagerduty.com/users/PXYZ789",
                "html_url": "https://app.pagerduty.com/users/PXYZ789",
                "time_zone": "Europe/London",
                "color": "purple",
                "role": "limited_user",
                "avatar_url": "https://secure.gravatar.com/avatar/123",
                "invitation_sent": false,
                "job_title": "Engineering Lead",
                "teams": [],
                "contact_methods": [],
                "notification_rules": [],
                "futureField": "futureValue"
            }
            """;

        var user = objectMapper.readValue(json, PagerDutyUser.class);

        assertEquals("PXYZ789", user.id());
        assertEquals("Jane Smith", user.name());
        assertEquals("jane@example.com", user.email());
        assertEquals("Europe/London", user.timeZone());
        assertEquals(Boolean.FALSE, user.invitationSent());

        // Unknown fields are now captured via @JsonCreator
        assertTrue(user.hasUnknownFields());
        assertEquals("futureValue", user.getUnknownField("futureField"));
    }

    @Test
    void testDeserializationWithMissingOptionalFields() throws Exception {
        String minimalJson = """
            {
                "id": "ABC123",
                "type": "user"
            }
            """;

        var user = objectMapper.readValue(minimalJson, PagerDutyUser.class);

        assertEquals("ABC123", user.id());
        assertEquals("user", user.type());
        assertNull(user.name());
        assertNull(user.email());
        assertFalse(user.hasUnknownFields());
    }
}