package com.booking.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Paginated response envelope. Carries the content list alongside Spring Data
 * pagination metadata so callers never need to inspect separate headers.
 *
 * <pre>
 * {
 *   "success": true,
 *   "content": [...],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 150,
 *   "totalPages": 8,
 *   "last": false,
 *   "timestamp": "2024-06-01T10:00:00Z"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {

    private boolean success;
    private String message;
    private List<T> content;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    @Builder.Default
    private Instant timestamp = Instant.now();

    // ── Factory helpers ──────────────────────────────────────────────────────

    /**
     * Builds a {@code PagedResponse} directly from a Spring Data {@link Page}.
     */
    public static <T> PagedResponse<T> of(Page<T> pageResult) {
        return PagedResponse.<T>builder()
                .success(true)
                .content(pageResult.getContent())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    public static <T> PagedResponse<T> of(Page<T> pageResult, String message) {
        PagedResponse<T> response = of(pageResult);
        response.setMessage(message);
        return response;
    }
}
