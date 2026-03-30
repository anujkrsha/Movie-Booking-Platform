package com.booking.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RFC 7807 Problem Details — error envelope returned by the global exception handler.
 *
 * <pre>
 * {
 *   "type":   "https://booking.com/errors/resource-not-found",
 *   "title":  "Resource Not Found",
 *   "status": 404,
 *   "detail": "Movie with id 42 was not found",
 *   "instance": "/api/v1/movies/42",
 *   "errors": { "title": ["must not be blank"] },
 *   "timestamp": "2024-06-01T10:00:00Z"
 * }
 * </pre>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** URI reference identifying the problem type. */
    private String type;

    /** Short, human-readable summary of the problem. */
    private String title;

    /** HTTP status code. */
    private int status;

    /** Human-readable explanation of this specific occurrence. */
    private String detail;

    /** URI reference of the specific request that triggered the problem. */
    private String instance;

    /**
     * Field-level validation errors.
     * Key = field name, Value = list of constraint violation messages.
     */
    private Map<String, List<String>> errors;

    @Builder.Default
    private Instant timestamp = Instant.now();

    // ── Factory helpers ──────────────────────────────────────────────────────

    public static ErrorResponse of(int status, String title, String detail, String instance) {
        return ErrorResponse.builder()
                .status(status)
                .title(title)
                .detail(detail)
                .instance(instance)
                .build();
    }

    public static ErrorResponse of(int status, String title, String detail,
                                   String instance, Map<String, List<String>> errors) {
        return ErrorResponse.builder()
                .status(status)
                .title(title)
                .detail(detail)
                .instance(instance)
                .errors(errors)
                .build();
    }
}
