package com.booking.movie.infrastructure.elasticsearch.repository;

import com.booking.movie.infrastructure.elasticsearch.document.MovieDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Elasticsearch repository for {@link MovieDocument}.
 *
 * <p>Basic CRUD (save, findById, delete) is provided by the framework.
 * Complex filtered searches use {@link org.springframework.data.elasticsearch.core.ElasticsearchOperations}
 * with a {@link com.booking.movie.infrastructure.elasticsearch.query.MovieSearchQuery NativeQuery}.
 */
@Repository
public interface MovieElasticsearchRepository extends ElasticsearchRepository<MovieDocument, String> {
}
