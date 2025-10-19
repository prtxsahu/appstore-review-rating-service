// MOVED TO: com.example.appstore.comment.domain.Comment
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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

/**
 * Comment entity representing a comment or reply on an app.
 * Composite key: app_id (PK) + comment_id (SK)
 * GSI: parent_index on parent_id for threaded comments
 * Note: deleted field removed as per updated design.
 */
/*
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Comment {
    // Implementation moved to com.example.appstore.comment.domain.Comment
}
*/