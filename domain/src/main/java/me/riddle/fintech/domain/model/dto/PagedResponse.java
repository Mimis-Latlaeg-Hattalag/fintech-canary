package me.riddle.fintech.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Generic paged response wrapper for PagerDuty API responses.
 * Handles pagination metadata and data collection.
 *
 * @param <T> The type of items in the paginated collection
 */
public record PagedResponse<T>(
        @JsonProperty("limit") int limit,
        @JsonProperty("offset") int offset,
        @JsonProperty("more") boolean more,
        @JsonProperty("total") Integer total,
        List<T> data
) {

    public PagedResponse {
        Objects.requireNonNull(data, "Data collection cannot be null");

        if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }

        // Defensive copy
        data = List.copyOf(data);
    }

    /**
     * Check if there are more pages available.
     */
    public boolean hasMorePages() {
        return more;
    }

    /**
     * Get the number of items in this page.
     */
    public int itemCount() {
        return data.size();
    }

    /**
     * Check if this page is empty.
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Calculate the next offset for pagination.
     */
    public int nextOffset() {
        return offset + limit;
    }

    /**
     * Calculate the previous offset for pagination.
     */
    public int previousOffset() {
        return Math.max(0, offset - limit);
    }

    /**
     * Check if this is the first page.
     */
    public boolean isFirstPage() {
        return offset == 0;
    }

    /**
     * Calculate estimated total pages (if total is available).
     */
    public Integer estimatedTotalPages() {
        if (total == null || limit == 0) {
            return null;
        }
        return (int) Math.ceil((double) total / limit);
    }

    /**
     * Calculate current page number (1-based).
     */
    public int currentPageNumber() {
        return (offset / limit) + 1;
    }
}