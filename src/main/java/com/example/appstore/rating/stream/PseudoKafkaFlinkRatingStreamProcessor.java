package com.example.appstore.rating.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.example.appstore.rating.domain.Aggregate;
import com.example.appstore.rating.repository.dynamodb.AggregateRepository;
import com.example.appstore.app.repository.elasticsearch.AppSearchRepository;
import com.example.appstore.rating.service.RatingService;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Concrete implementation of both RatingObserver and RatingStreamProcessor.
 * This class acts as an observer of the rating service and processes rating events
 * in a pseudo Kafka-Flink stream environment.
 */
@Slf4j
@Component
public class PseudoKafkaFlinkRatingStreamProcessor implements RatingObserver, RatingStreamProcessor {

    private final RatingService ratingService;
    private final AggregateRepository aggregateRepository;
    private final AppSearchRepository appSearchRepository;

    // Local rating aggregation: appId -> RatingAggregate(count, sum)
    private final ConcurrentHashMap<String, RatingAggregate> ratingAggregates;

    // Set of app_ids that need to be updated in DB
    private final Set<String> appsToUpdate;

    // Timer for periodic DB flush
    private final ScheduledExecutorService scheduler;
    private final ScheduledFuture<?> flushTask;

    /**
     * Constructor initializes the stream processor.
     */
    public PseudoKafkaFlinkRatingStreamProcessor(RatingService ratingService, 
                                               AggregateRepository aggregateRepository,
                                               AppSearchRepository appSearchRepository) {
        this.ratingService = ratingService;
        this.aggregateRepository = aggregateRepository;
        this.appSearchRepository = appSearchRepository;
        this.ratingAggregates = new ConcurrentHashMap<>();
        this.appsToUpdate = ConcurrentHashMap.newKeySet();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // Start the 30-second timer for DB flush
        this.flushTask = scheduler.scheduleAtFixedRate(
                this::flushToDatabase,
                30, 30, TimeUnit.SECONDS
        );
    }

    /**
     * Register this observer with the RatingService after Spring initialization.
     */
    @PostConstruct
    public void registerWithRatingService() {
        ratingService.register(this);
        log.info("PseudoKafkaFlinkRatingStreamProcessor registered with RatingService");
    }
    
    // ==================== RatingObserver Implementation ====================
    
    @Override
    public void onRatingCreated(String appId, String userId, Integer ratingValue, Instant timestamp) {
        log.debug("Rating created - appId: {}, userId: {}, value: {}", appId, userId, ratingValue);
        updateRatingAggregate(appId, userId, null, ratingValue);
    }
    
    @Override
    public void onRatingUpdated(String appId, String userId, Integer oldRatingValue, Integer newRatingValue, Instant timestamp) {
        log.debug("Rating updated - appId: {}, userId: {}, oldValue: {}, newValue: {}", 
                appId, userId, oldRatingValue, newRatingValue);
        updateRatingAggregate(appId, userId, oldRatingValue, newRatingValue);
    }
    
    @Override
    public void onRatingDeleted(String appId, String userId, Integer deletedRatingValue, Instant timestamp) {
        log.debug("Rating deleted - appId: {}, userId: {}, value: {}", appId, userId, deletedRatingValue);
        updateRatingAggregate(appId, userId, deletedRatingValue, null);
    }
    
    @Override
    public void onRatingError(String appId, String userId, Throwable error) {
        log.error("Rating error - appId: {}, userId: {}", appId, userId, error);
    }
    
    // ==================== RatingStreamProcessor Implementation ====================
    
    @Override
    public CompletableFuture<Void> start() {
        log.info("Rating stream processor started");
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> stop() {
        log.info("Stopping rating stream processor");
        flushTask.cancel(false);
        scheduler.shutdown();
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public boolean isActive() {
        return !scheduler.isShutdown();
    }
    
    @Override
    public StreamStatistics getStatistics() {
        return new StreamStatistics(
                0, 0, 0, 
                ratingAggregates.size(), 
                isActive()
        );
    }
    
    @Override
    public RatingAggregation getAppRatingAggregation(String appId) {
        RatingAggregate aggregate = ratingAggregates.get(appId);
        if (aggregate != null) {
            return new RatingAggregation(
                    appId,
                    aggregate.getAverageRating(),
                    aggregate.getCount(),
                    aggregate.getCount()
            );
        }
        return null;
    }
    
    // ==================== Private Methods ====================
    
    /**
     * Update rating aggregate for an app.
     * 
     * @param appId The app ID
     * @param userId The user ID  
     * @param oldRating The old rating value (null for create, actual value for update/delete)
     * @param newRating The new rating value (null for delete, actual value for create/update)
     */
    private void updateRatingAggregate(String appId, String userId, Integer oldRating, Integer newRating) {
        RatingAggregate aggregate = ratingAggregates.computeIfAbsent(appId, k -> new RatingAggregate());
        
        // Update the aggregate
        if (oldRating != null) {
            aggregate.removeRating(oldRating); // Remove old rating
        }
        if (newRating != null) {
            aggregate.addRating(newRating); // Add new rating
        }
        
        // Add app to the set that needs DB update
        appsToUpdate.add(appId);
        
        log.debug("Updated aggregate for appId: {} - count: {}, sum: {}, avg: {}", 
                appId, aggregate.getCount(), aggregate.getSum(), aggregate.getAverageRating());
    }
    
    /**
     * Flush all pending updates to database (called every 30 seconds).
     */
    private void flushToDatabase() {
        if (appsToUpdate.isEmpty()) {
            log.debug("No apps to update in database");
            return;
        }
        
        // this is done to make the process of getting all appIds in set and cleaning it up, thread safe.
        Set<String> appsToFlush;
        synchronized (appsToUpdate) {
            appsToFlush = new HashSet<>(appsToUpdate);
            appsToUpdate.clear();
        }
        
        log.info("Starting DB flush for {} apps", appsToFlush.size());
        
        // Flush each app individually
        for (String appId : appsToFlush) {
            try {
                flushAppToDatabase(appId);
            } catch (Exception e) {
                log.error("Failed to flush app {} to database", appId, e);
                // Re-add to set for retry in next cycle
                appsToUpdate.add(appId);
            }
        }
        
        log.info("Completed DB flush for {} apps", appsToFlush.size());
    }
    
    /**
     * Flush a single app's rating aggregate to both DynamoDB and Elasticsearch.
     * Performs dual-write operation for data consistency.
     * 
     * @param appId The app ID to flush
     */
    private void flushAppToDatabase(String appId) {
        RatingAggregate ratingAggregate = ratingAggregates.get(appId);
        if (ratingAggregate == null) {
            log.warn("No aggregate found for appId: {}", appId);
            return;
        }
        
        double avgRating = ratingAggregate.getAverageRating();
        
        // 1. Update DynamoDB Aggregates table
        boolean dynamoDBSuccess = updateDynamoDBAggregate(appId, ratingAggregate);
        
        // 2. Update Elasticsearch App index
        boolean elasticsearchSuccess = updateElasticsearchApp(appId, avgRating);
        
        // 3. Handle results
        if (dynamoDBSuccess && elasticsearchSuccess) {
            log.info("Dual-write completed successfully - appId: {}, count: {}, sum: {}, avg: {}", 
                    appId, ratingAggregate.getCount(), ratingAggregate.getSum(), avgRating);
        } else if (dynamoDBSuccess || elasticsearchSuccess) {
            log.warn("Partial success for appId: {} - DynamoDB: {}, Elasticsearch: {}", 
                    appId, dynamoDBSuccess, elasticsearchSuccess);
            // Re-add to set for retry of failed operation
            appsToUpdate.add(appId);
        } else {
            log.error("Both DynamoDB and Elasticsearch updates failed for appId: {}", appId);
            // Re-add to set for retry
            appsToUpdate.add(appId);
        }
    }
    
    /**
     * Update DynamoDB Aggregates table with rating data.
     * 
     * @param appId The application ID
     * @param ratingAggregate The rating aggregate data
     * @return true if successful, false otherwise
     */
    private boolean updateDynamoDBAggregate(String appId, RatingAggregate ratingAggregate) {
        try {
            Aggregate aggregate = Aggregate.builder()
                    .appId(appId)
                    .totalCount(ratingAggregate.getCount())
                    .totalSum(ratingAggregate.getSum())
                    .lastUpdated(Instant.now())
                    .build();
            
            aggregateRepository.save(aggregate);
            log.debug("Successfully updated DynamoDB aggregate for appId: {}", appId);
            return true;
        } catch (Exception e) {
            log.error("Failed to update DynamoDB aggregate for appId: {}", appId, e);
            return false;
        }
    }
    
    /**
     * Update Elasticsearch App index with new average rating.
     * 
     * @param appId The application ID
     * @param avgRating The new average rating
     * @return true if successful, false otherwise
     */
    private boolean updateElasticsearchApp(String appId, double avgRating) {
        try {
            appSearchRepository.updateAppRating(appId, avgRating);
            log.debug("Successfully updated Elasticsearch app rating for appId: {}", appId);
            return true;
        } catch (Exception e) {
            log.error("Failed to update Elasticsearch app rating for appId: {}", appId, e);
            return false;
        }
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Simple rating aggregate with count and sum.
     */
    private static class RatingAggregate {
        private volatile long count;
        private volatile long sum;
        
        public RatingAggregate() {
            this.count = 0;
            this.sum = 0;
        }
        
        public synchronized void addRating(Integer rating) {
            count++;
            sum += rating;
        }
        
        public synchronized void removeRating(Integer rating) {
            if (count > 0) {
                count--;
                sum -= rating;
                if (count == 0) {
                    sum = 0;
                }
            }
        }
        
        public double getAverageRating() {
            return count > 0 ? (double) sum / count : 0.0;
        }
        
        // Getters
        public long getCount() { return count; }
        public long getSum() { return sum; }
    }
}
