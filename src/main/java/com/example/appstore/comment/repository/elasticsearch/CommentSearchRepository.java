package com.example.appstore.comment.repository.elasticsearch;

import com.example.appstore.comment.domain.Comment;
import com.example.appstore.shared.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Repository for Comment search operations using OpenSearch/Elasticsearch.
 * Handles indexing and searching of comments for keyword-based search.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CommentSearchRepository {

    private final ElasticsearchClient elasticsearchClient;
    private static final String INDEX_NAME = "comments_index";

    /**
     * Create the comments index if it doesn't exist.
     */
    public void createIndexIfNotExists() {
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(INDEX_NAME));
            boolean exists = elasticsearchClient.indices().exists(existsRequest).value();
            
            if (!exists) {
                log.info("Creating comments index: {}", INDEX_NAME);
                CreateIndexRequest createRequest = CreateIndexRequest.of(c -> c
                        .index(INDEX_NAME)
                        .mappings(m -> m
                                .properties("commentId", p -> p.keyword(k -> k))
                                .properties("appId", p -> p.keyword(k -> k))
                                .properties("userId", p -> p.keyword(k -> k))
                                .properties("parentId", p -> p.keyword(k -> k))
                                .properties("text", p -> p.text(t -> t.analyzer("standard")))
                                .properties("createdAt", p -> p.date(d -> d))
                                .properties("updatedAt", p -> p.date(d -> d))
                                .properties("subCommentCount", p -> p.long_(l -> l))
                                .properties("likesCount", p -> p.long_(l -> l))
                                .properties("dislikesCount", p -> p.long_(l -> l))
                        )
                );
                elasticsearchClient.indices().create(createRequest);
                log.info("Successfully created comments index: {}", INDEX_NAME);
            } else {
                log.debug("Comments index already exists: {}", INDEX_NAME);
            }
        } catch (IOException e) {
            log.error("Error creating comments index: {}", INDEX_NAME, e);
            throw new RuntimeException("Failed to create comments index", e);
        }
    }

    /**
     * Index a comment document for search.
     */
    public void indexComment(Comment comment) {
        try {
            log.debug("Indexing comment: {}", comment.getCommentId());
            
            // Create a map for the document to avoid serialization issues
            java.util.Map<String, Object> docMap = new java.util.HashMap<>();
            docMap.put("commentId", comment.getCommentId());
            docMap.put("appId", comment.getAppId());
            docMap.put("userId", comment.getUserId());
            docMap.put("parentId", comment.getParentId());
            docMap.put("text", comment.getText());
            docMap.put("createdAt", comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : null);
            docMap.put("updatedAt", comment.getUpdatedAt() != null ? comment.getUpdatedAt().toString() : null);
            docMap.put("subCommentCount", comment.getSubCommentCount());
            docMap.put("likesCount", comment.getLikesCount());
            docMap.put("dislikesCount", comment.getDislikesCount());
            
            IndexRequest<java.util.Map<String, Object>> indexRequest = IndexRequest.of(i -> i
                    .index(INDEX_NAME)
                    .id(comment.getCommentId())
                    .document(docMap)
            );
            
            IndexResponse response = elasticsearchClient.index(indexRequest);
            log.info("Successfully indexed comment: {} with version: {}", comment.getCommentId(), response.version());
        } catch (IOException e) {
            log.error("Error indexing comment: {}", comment.getCommentId(), e);
            throw new RuntimeException("Failed to index comment", e);
        }
    }



    /**
     * Search comments within a specific app by keyword with pagination.
     */
    public SearchResponse<Comment> searchCommentsByAppId(String appId, String keyword, int page, int size) {
        try {
            log.debug("Searching comments in app {} with keyword: {}, page: {}, size: {}", appId, keyword, page, size);
            
            Query query = Query.of(q -> q
                    .bool(BoolQuery.of(b -> b
                            .must(must -> must
                                    .match(m -> m
                                            .field("text")
                                            .query(keyword)
                                            .fuzziness("AUTO")
                                    )
                            )
                            .filter(filter -> filter
                                    .term(t -> t
                                            .field("appId")
                                            .value(appId)
                                    )
                            )
                    ))
            );
            
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(query)
                    .from(page * size)
                    .size(size)
                    .sort(sort -> sort
                            .score(sc -> sc.order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))
                    )
            );
            
            // Search and get raw results
            co.elastic.clients.elasticsearch.core.SearchResponse<java.util.Map> response = 
                    elasticsearchClient.search(searchRequest, java.util.Map.class);
            
            // Convert Map results to Comment domain objects
            List<Comment> comments = response.hits().hits().stream()
                    .map(hit -> convertMapToComment((Map<String, Object>) hit.source()))
                    .filter(comment -> comment != null)
                    .toList();
            
            // Calculate pagination info
            long totalElements = response.hits().total() != null ? response.hits().total().value() : 0;
            int totalPages = (int) Math.ceil((double) totalElements / size);
            boolean hasNext = page < totalPages - 1;
            boolean hasPrevious = page > 0;
            
            // Build our DTO response
            SearchResponse<Comment> searchResponse = SearchResponse.<Comment>builder()
                    .items(comments)
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .hasNext(hasNext)
                    .hasPrevious(hasPrevious)
                    .build();
            
            log.info("Found {} comments in app {} for keyword: {}", comments.size(), appId, keyword);
            return searchResponse;
        } catch (IOException e) {
            log.error("Error searching comments in app {} with keyword: {}", appId, keyword, e);
            throw new RuntimeException("Failed to search comments by app", e);
        }
    }

    /**
     * Delete a comment from the search index.
     */
    public void deleteComment(String commentId) {
        try {
            log.debug("Deleting comment from search index: {}", commentId);
            elasticsearchClient.delete(d -> d
                    .index(INDEX_NAME)
                    .id(commentId)
            );
            log.info("Successfully deleted comment from search index: {}", commentId);
        } catch (IOException e) {
            log.error("Error deleting comment from search index: {}", commentId, e);
            throw new RuntimeException("Failed to delete comment from search index", e);
        }
    }

    /**
     * Update a comment in the search index.
     */
    public void updateComment(Comment comment) {
        // For OpenSearch, update is the same as index (upsert behavior)
        indexComment(comment);
    }

    /**
     * Convert Map from Elasticsearch to Comment domain object.
     */
    private Comment convertMapToComment(Map<String, Object> docMap) {
        if (docMap == null) {
            return null;
        }

        try {
            return Comment.builder()
                    .commentId((String) docMap.get("commentId"))
                    .appId((String) docMap.get("appId"))
                    .userId((String) docMap.get("userId"))
                    .parentId((String) docMap.get("parentId"))
                    .text((String) docMap.get("text"))
                    .createdAt(docMap.get("createdAt") != null ? 
                            Instant.parse((String) docMap.get("createdAt")) : null)
                    .updatedAt(docMap.get("updatedAt") != null ? 
                            Instant.parse((String) docMap.get("updatedAt")) : null)
                    .subCommentCount(docMap.get("subCommentCount") != null ? 
                            ((Number) docMap.get("subCommentCount")).longValue() : 0L)
                    .likesCount(docMap.get("likesCount") != null ? 
                            ((Number) docMap.get("likesCount")).longValue() : 0L)
                    .dislikesCount(docMap.get("dislikesCount") != null ? 
                            ((Number) docMap.get("dislikesCount")).longValue() : 0L)
                    .build();
        } catch (Exception e) {
            log.error("Error converting map to comment: {}", e.getMessage(), e);
            return null;
        }
    }
}
