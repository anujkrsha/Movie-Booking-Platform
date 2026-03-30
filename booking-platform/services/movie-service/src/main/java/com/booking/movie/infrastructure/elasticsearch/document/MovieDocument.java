package com.booking.movie.infrastructure.elasticsearch.document;

import com.booking.movie.domain.entity.MovieStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch document for the {@code movies} index.
 *
 * <p>Field strategy:
 * <ul>
 *   <li>{@code title}       — {@code text} for full-text + {@code keyword} sub-field for sorting.</li>
 *   <li>{@code language}, {@code genre}, {@code status}, {@code cities}
 *                           — {@code keyword} for exact-match filters.</li>
 *   <li>{@code releaseDate} — {@code date} (yyyy-MM-dd) for range queries.</li>
 *   <li>{@code showCount}   — populated by the daily sync from show-service data.</li>
 * </ul>
 */
@Document(indexName = "movies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDocument {

    @Id
    private String id;  // Movie UUID as string

    @MultiField(
        mainField  = @Field(type = FieldType.Text,    analyzer = "standard"),
        otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String title;

    @Field(type = FieldType.Keyword)
    private String language;

    @Field(type = FieldType.Keyword)
    private String genre;

    @Field(type = FieldType.Integer)
    private Integer durationMins;

    @Field(type = FieldType.Double)
    private BigDecimal rating;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate releaseDate;

    @Field(type = FieldType.Text, index = false)
    private String posterUrl;

    @Field(type = FieldType.Text)
    private String description;

    /** Lifecycle status; search filters on {@code RELEASED} only. */
    @Field(type = FieldType.Keyword)
    private String status;

    /**
     * Cities where the movie is currently showing.
     * Populated by the daily sync scheduler from show-service data.
     */
    @Field(type = FieldType.Keyword)
    @Builder.Default
    private List<String> cities = new ArrayList<>();

    /**
     * Total number of active shows scheduled for this movie.
     * Updated daily by {@link com.booking.movie.infrastructure.elasticsearch.sync.MovieSyncScheduler}.
     */
    @Field(type = FieldType.Integer)
    @Builder.Default
    private int showCount = 0;
}
