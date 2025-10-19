// MOVED TO: com.example.appstore.rating.domain.Aggregate
// This file has been moved to the new feature-based structure
// TODO: Remove this file after confirming the refactoring is complete

/*
package com.example.appstore.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

/**
 * Aggregate entity for storing rating aggregation data.
 * This table stores the periodic persistence of in-memory aggregator data.
 * Used for aggregator recovery on service restart.
 */
/*
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Aggregate {

    private String appId;
    private Long totalSum;
    private Long totalCount;
    private Instant lastUpdated;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("app_id")
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @DynamoDbAttribute("total_sum")
    public Long getTotalSum() {
        return totalSum;
    }

    public void setTotalSum(Long totalSum) {
        this.totalSum = totalSum;
    }

    @DynamoDbAttribute("total_count")
    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    @DynamoDbAttribute("last_updated")
    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
*/