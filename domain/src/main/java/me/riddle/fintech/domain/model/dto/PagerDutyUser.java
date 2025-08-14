package me.riddle.fintech.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * PagerDuty User domain model.
 * Designed for extensibility - gracefully handles unknown fields for API evolution.
 * <p>
 * FixMe: revisit unused after actual tests implementation
 */
@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagerDutyUser(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("summary") String summary,
        @JsonProperty("type") String type,
        @JsonProperty("self") String self,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("avatar_url") String avatarUrl,
        @JsonProperty("color") String color,
        @JsonProperty("role") String role,
        @JsonProperty("description") String description,
        @JsonProperty("invitation_sent") Boolean invitationSent,
        @JsonProperty("job_title") String jobTitle,
        @JsonProperty("time_zone") String timeZone,

        // Extensibility: capture unknown fields for forward compatibility
        Map<String, Object> unknownFields
) {

    public PagerDutyUser {
        Objects.requireNonNull(id, "User ID cannot be null");
        Objects.requireNonNull(type, "User type cannot be null");

        // Defensive copy for unknown fields
        unknownFields = unknownFields != null ? Map.copyOf(unknownFields) : Map.of();
    }

    /**
     * Constructor for required fields only - other fields default to null.
     */
    public PagerDutyUser(String id, String name, String email, String type) {
        this(id, name, email, null, type, null, null, null, null, null, null, null, null, null, Map.of());
    }

    /**
     * Jackson deserialization support for unknown fields.
     */
    @JsonAnyGetter
    public Map<String, Object> unknownFields() {
        return unknownFields;
    }

    /**
     * Handle unknown fields during deserialization.
     */
    @JsonAnySetter
    public PagerDutyUser withUnknownField(String key, Object value) {
        var newUnknownFields = new HashMap<>(this.unknownFields);
        newUnknownFields.put(key, value);
        return new PagerDutyUser(id, name, email, summary, type, self, htmlUrl, avatarUrl,
                color, role, description, invitationSent, jobTitle, timeZone,
                newUnknownFields);
    }

    /**
     * Check if this user has any unknown/future fields.
     * Useful for detecting API changes.
     */
    public boolean hasUnknownFields() {
        return !unknownFields.isEmpty();
    }

    /**
     * Get a specific unknown field by name.
     */
    public Object getUnknownField(String fieldName) {
        return unknownFields.get(fieldName);
    }
}