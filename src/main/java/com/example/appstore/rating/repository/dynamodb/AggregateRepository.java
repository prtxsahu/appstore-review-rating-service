package com.example.appstore.rating.repository.dynamodb;

import com.example.appstore.rating.domain.Aggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for Aggregate entity operations using DynamoDB Enhanced Client.
 * Used for rating aggregation data persistence and recovery.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AggregateRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String TABLE_NAME = "aggregates";

    private DynamoDbTable<Aggregate> getAggregateTable() {
        return dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromBean(Aggregate.class));
    }

    /**
     * Save aggregate data to DynamoDB.
     */
    public Aggregate save(Aggregate aggregate) {
        try {
            log.debug("Saving aggregate for app: {}", aggregate.getAppId());
            DynamoDbTable<Aggregate> aggregateTable = getAggregateTable();
            aggregateTable.putItem(aggregate);
            log.info("Successfully saved aggregate for app: {}", aggregate.getAppId());
            return aggregate;
        } catch (DynamoDbException e) {
            log.error("Error saving aggregate for app: {}", aggregate.getAppId(), e);
            throw new RuntimeException("Failed to save aggregate", e);
        }
    }

    /**
     * Find aggregate data by app ID.
     */
    public Optional<Aggregate> findByAppId(String appId) {
        try {
            log.debug("Finding aggregate for app: {}", appId);
            DynamoDbTable<Aggregate> aggregateTable = getAggregateTable();
            Key key = Key.builder()
                    .partitionValue(appId)
                    .build();
            
            GetItemEnhancedRequest request = GetItemEnhancedRequest.builder()
                    .key(key)
                    .build();
            
            Aggregate aggregate = aggregateTable.getItem(request);
            if (aggregate != null) {
                log.info("Found aggregate for app: {}", appId);
                return Optional.of(aggregate);
            } else {
                log.info("Aggregate not found for app: {}", appId);
                return Optional.empty();
            }
        } catch (DynamoDbException e) {
            log.error("Error finding aggregate for app: {}", appId, e);
            throw new RuntimeException("Failed to find aggregate", e);
        }
    }

    /**
     * Find all aggregates (used for aggregator recovery on startup).
     */
    public List<Aggregate> findAll() {
        try {
            log.debug("Finding all aggregates for recovery");
            DynamoDbTable<Aggregate> aggregateTable = getAggregateTable();
            
            return aggregateTable.scan()
                    .items()
                    .stream()
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            log.error("Error finding all aggregates", e);
            throw new RuntimeException("Failed to find all aggregates", e);
        }
    }
}
