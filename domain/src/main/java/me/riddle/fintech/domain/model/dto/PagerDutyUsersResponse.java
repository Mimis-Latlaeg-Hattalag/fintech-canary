package me.riddle.fintech.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * PagerDuty-specific response for user listing endpoint.
 * Handles the "users" wrapper field in the API response.
 * Use classic approach with manual sliding windows:
 * (Gracefully copied from Rest Template original implementation)
 */
public record PagerDutyUsersResponse(
    @JsonProperty("users") List<PagerDutyUser> users,
    @JsonProperty("limit") int limit,
    @JsonProperty("offset") int offset,
    @JsonProperty("more") boolean more,
    @JsonProperty("total") Integer total
) {

    public PagerDutyUsersResponse {
        users = users != null ? List.copyOf(users) : List.of();

        if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }
    }

    /**
     * Convert to generic PagedResponse for easier handling.
     */
    public PagedResponse<PagerDutyUser> toPagedResponse() {
        return new PagedResponse<>(limit, offset, more, total, users);
    }

    /**
     * Check if there are more pages available.
     */
    public boolean hasMorePages() {
        return more;
    }

    /**
     * Get the number of users in this response.
     */
    public int userCount() {
        return users.size();
    }

    /**
     * Check if this response is empty.
     */
    public boolean isEmpty() {
        return users.isEmpty();
    }

    /**
     * Calculate the next offset for pagination.
     */
    public int nextOffset() {
        return offset + limit;
    }

    /**
     * Get pagination metadata as string for logging.
     */
    public String getPaginationLoggingInfo() {
        return String.format("offset=%d, limit=%d, count=%d, more=%s, total=%s",
                           offset, limit, users.size(), more, total);
    }
}