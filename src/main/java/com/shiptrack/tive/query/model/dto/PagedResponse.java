package com.shiptrack.tive.query.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response envelope.
 *
 * Wraps any list of items with page metadata so callers can implement
 * cursor/offset-based pagination without inspecting HTTP headers.
 *
 * @param <T> the item type contained in this page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> items;

    /** Zero-based index of the current page. */
    private int page;

    /** Number of items requested per page. */
    private int size;

    /** Total number of items matching the query (across all pages). */
    private long totalItems;

    /** Total number of pages. */
    private int totalPages;

    /** Whether this is the last page. */
    private boolean last;
}

