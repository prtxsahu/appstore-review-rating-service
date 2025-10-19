// MOVED TO: com.example.appstore.user.repository.dynamodb.UserRepository
// This file has been moved to the new feature-based structure
// TODO: Remove this file after confirming the refactoring is complete

/*
package com.example.appstore.repository.dynamodb;

import com.example.appstore.domain.User;
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
 * Repository for User entity operations using DynamoDB Enhanced Client.
 * Implements Repository pattern for data access abstraction.
 */
/*
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String TABLE_NAME = "users";

    private DynamoDbTable<User> getUserTable() {
        return dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromBean(User.class));
    }

    /**
     * Save a user to DynamoDB.
     */
    /*
    public User save(User user) {
        log.info("=== USER REPOSITORY: Saving user to DynamoDB ===");
        log.info("Input - userId: {}, name: {}", user.getUserId(), user.getName());
        log.debug("Full user entity: {}", user);
        
        try {
            DynamoDbTable<User> userTable = getUserTable();
            log.debug("Retrieved DynamoDB table: {}", TABLE_NAME);
            userTable.putItem(user);
            log.info("User saved successfully to DynamoDB - userId: {}", user.getUserId());
            log.debug("Saved user entity: {}", user);
            return user;
        } catch (DynamoDbException e) {
            log.error("DynamoDB error saving user - userId: {}, error: {}", user.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save user", e);
        } catch (Exception e) {
            log.error("Unexpected error saving user - userId: {}, error: {}", user.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save user", e);
        }
    }

    /**
     * Find a user by ID.
     */
    /*
    public Optional<User> findById(String userId) {
        log.info("=== USER REPOSITORY: Finding user by ID ===");
        log.info("Input - userId: {}", userId);
        
        try {
            DynamoDbTable<User> userTable = getUserTable();
            log.debug("Retrieved DynamoDB table: {}", TABLE_NAME);
            
            Key key = Key.builder()
                    .partitionValue(userId)
                    .build();
            log.debug("Built DynamoDB key: {}", key);
            
            GetItemEnhancedRequest request = GetItemEnhancedRequest.builder()
                    .key(key)
                    .build();
            log.debug("Built GetItemEnhancedRequest");
            
            User user = userTable.getItem(request);
            if (user != null) {
                log.info("User found in DynamoDB - userId: {}, name: {}", user.getUserId(), user.getName());
                log.debug("Retrieved user entity: {}", user);
                return Optional.of(user);
            } else {
                log.info("User not found in DynamoDB - userId: {}", userId);
                return Optional.empty();
            }
        } catch (DynamoDbException e) {
            log.error("DynamoDB error finding user - userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to find user", e);
        } catch (Exception e) {
            log.error("Unexpected error finding user - userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to find user", e);
        }
    }

    /**
     * Check if a user exists by ID.
     */
    /*
    public boolean existsById(String userId) {
        log.info("=== USER REPOSITORY: Checking if user exists ===");
        log.info("Input - userId: {}", userId);
        
        try {
            boolean exists = findById(userId).isPresent();
            log.info("User existence check completed - userId: {}, exists: {}", userId, exists);
            return exists;
        } catch (Exception e) {
            log.error("Error checking user existence - userId: {}, error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Find all users (use with caution for large datasets).
     */
    /*
    public List<User> findAll() {
        try {
            log.debug("Finding all users");
            DynamoDbTable<User> userTable = getUserTable();
            
            return userTable.scan()
                    .items()
                    .stream()
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            log.error("Error finding all users", e);
            throw new RuntimeException("Failed to find all users", e);
        }
    }

    /**
     * Delete a user by ID.
     */
    /*
    public void deleteById(String userId) {
        try {
            log.debug("Deleting user: {}", userId);
            DynamoDbTable<User> userTable = getUserTable();
            Key key = Key.builder()
                    .partitionValue(userId)
                    .build();
            
            userTable.deleteItem(key);
            log.info("Successfully deleted user: {}", userId);
        } catch (DynamoDbException e) {
            log.error("Error deleting user: {}", userId, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }
}
*/