package me.riddle.fintech.domain.model.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PagedResponseTest {

    @Test
    void testConstructorValidation() {
        // Null data collection
        assertThrows(NullPointerException.class, () ->
                new PagedResponse<>(10, 0, false, null, null));

        // Negative limit
        assertThrows(IllegalArgumentException.class, () ->
                new PagedResponse<>(-1, 0, false, null, List.of()));

        // Negative offset
        assertThrows(IllegalArgumentException.class, () ->
                new PagedResponse<>(10, -1, false, null, List.of()));
    }

    @Test
    void testDefensiveCopyOfData() {
        var mutableList = new ArrayList<String>();
        mutableList.add("item1");
        mutableList.add("item2");

        var response = new PagedResponse<>(10, 0, false, null, mutableList);

        // Modify original list
        mutableList.add("item3");

        // Response data should be unchanged
        assertEquals(2, response.itemCount());
        assertEquals(List.of("item1", "item2"), response.data());
    }

    @Test
    void testHasMorePages() {
        var hasMore = new PagedResponse<>(10, 0, true, null, List.of("a", "b"));
        assertTrue(hasMore.hasMorePages());

        var noMore = new PagedResponse<>(10, 0, false, null, List.of("a", "b"));
        assertFalse(noMore.hasMorePages());
    }

    @Test
    void testItemCountAndIsEmpty() {
        var empty = new PagedResponse<>(10, 0, false, null, List.of());
        assertEquals(0, empty.itemCount());
        assertTrue(empty.isEmpty());

        var withItems = new PagedResponse<>(10, 0, false, null, List.of("a", "b", "c"));
        assertEquals(3, withItems.itemCount());
        assertFalse(withItems.isEmpty());
    }

    @Test
    void testNextOffset() {
        var response = new PagedResponse<>(25, 50, true, null, List.of("a"));
        assertEquals(75, response.nextOffset());
    }

    @Test
    void testPreviousOffset() {
        // Normal case
        var response1 = new PagedResponse<>(25, 50, false, null, List.of("a"));
        assertEquals(25, response1.previousOffset());

        // Edge case - would go negative
        var response2 = new PagedResponse<>(25, 10, false, null, List.of("a"));
        assertEquals(0, response2.previousOffset());

        // Already at start
        var response3 = new PagedResponse<>(25, 0, false, null, List.of("a"));
        assertEquals(0, response3.previousOffset());
    }

    @Test
    void testIsFirstPage() {
        var firstPage = new PagedResponse<>(10, 0, true, null, List.of("a"));
        assertTrue(firstPage.isFirstPage());

        var notFirstPage = new PagedResponse<>(10, 10, true, null, List.of("a"));
        assertFalse(notFirstPage.isFirstPage());
    }

    @Test
    void testEstimatedTotalPages() {
        // Normal case
        var response1 = new PagedResponse<>(25, 0, true, 100, List.of("a"));
        assertEquals(4, response1.estimatedTotalPages());

        // With remainder
        var response2 = new PagedResponse<>(25, 0, true, 101, List.of("a"));
        assertEquals(5, response2.estimatedTotalPages());

        // Total is null
        var response3 = new PagedResponse<>(25, 0, true, null, List.of("a"));
        assertNull(response3.estimatedTotalPages());

        // Limit is 0 (edge case)
        var response4 = new PagedResponse<>(0, 0, false, 100, List.of());
        assertNull(response4.estimatedTotalPages());
    }

    @Test
    void testCurrentPageNumber() {
        // First page (1-based)
        var page1 = new PagedResponse<>(10, 0, true, null, List.of("a"));
        assertEquals(1, page1.currentPageNumber());

        // Second page
        var page2 = new PagedResponse<>(10, 10, true, null, List.of("a"));
        assertEquals(2, page2.currentPageNumber());

        // Fifth page
        var page5 = new PagedResponse<>(10, 40, false, null, List.of("a"));
        assertEquals(5, page5.currentPageNumber());
    }

    @Test
    void testCurrentPageNumberWithZeroLimit() {
        // Edge case - limit is 0, should throw ArithmeticException
        var response = new PagedResponse<>(0, 0, false, null, List.of());
        assertThrows(ArithmeticException.class, response::currentPageNumber);
    }
}