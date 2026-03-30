package com.booking.movie.domain.repository;

import com.booking.movie.domain.entity.Movie;
import com.booking.movie.domain.entity.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    List<Movie> findByStatus(MovieStatus status);

    /** Returns all movies in batches for the daily ES sync (only non-archived). */
    @Query("SELECT m FROM Movie m WHERE m.status <> com.booking.movie.domain.entity.MovieStatus.ARCHIVED")
    List<Movie> findAllActive();
}
