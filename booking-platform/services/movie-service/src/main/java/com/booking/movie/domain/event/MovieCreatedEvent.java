package com.booking.movie.domain.event;

import com.booking.movie.domain.entity.Movie;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring {@link ApplicationEvent} published after a new {@link Movie} is persisted.
 *
 * <p>A {@code @TransactionalEventListener} picks this up after the transaction commits
 * and indexes the document into Elasticsearch.
 */
@Getter
public class MovieCreatedEvent extends ApplicationEvent {

    private final Movie movie;

    public MovieCreatedEvent(Movie movie) {
        super(movie);
        this.movie = movie;
    }
}
