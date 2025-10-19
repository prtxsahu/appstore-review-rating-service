// package com.example.appstore.stream;

// import java.util.concurrent.CompletableFuture;

// /**
//  * Interface for rating stream processing.
//  * Defines methods for processing rating events in a stream-like environment.
//  */
// public interface RatingStreamProcessor {
    
//     /**
//      * Start the stream processing.
//      * 
//      * @return CompletableFuture that completes when stream is started
//      */
//     CompletableFuture<Void> start();
    
//     /**
//      * Stop the stream processing.
//      * 
//      * @return CompletableFuture that completes when stream is stopped
//      */
//     CompletableFuture<Void> stop();
    
//     /**
//      * Check if the stream processing is currently active.
//      * 
//      * @return true if stream is active, false otherwise
//      */
//     boolean isActive();
    
//     /**
//      * Get statistics about the stream processing.
//      * 
//      * @return Stream processing statistics
//      */
//     StreamStatistics getStatistics();
    
//     /**
//      * Get the current rating aggregation for an app.
//      * 
//      * @param appId The application ID
//      * @return Rating aggregation data, or null if not found
//      */
//     RatingAggregation getAppRatingAggregation(String appId);
    
//     /**
//      * Statistics class for stream processing.
//      */
//     class StreamStatistics {
//         private final long processedEvents;
//         private final long failedEvents;
//         private final int queueSize;
//         private final int totalAppsWithRatings;
//         private final boolean isActive;
        
//         public StreamStatistics(long processedEvents, long failedEvents, int queueSize, 
//                               int totalAppsWithRatings, boolean isActive) {
//             this.processedEvents = processedEvents;
//             this.failedEvents = failedEvents;
//             this.queueSize = queueSize;
//             this.totalAppsWithRatings = totalAppsWithRatings;
//             this.isActive = isActive;
//         }
        
//         // Getters
//         public long getProcessedEvents() { return processedEvents; }
//         public long getFailedEvents() { return failedEvents; }
//         public int getQueueSize() { return queueSize; }
//         public int getTotalAppsWithRatings() { return totalAppsWithRatings; }
//         public boolean isActive() { return isActive; }
//     }
    
//     /**
//      * Rating aggregation data for an app.
//      */
//     class RatingAggregation {
//         private final String appId;
//         private final double averageRating;
//         private final long totalRatings;
//         private final long uniqueUsers;
        
//         public RatingAggregation(String appId, double averageRating, long totalRatings, long uniqueUsers) {
//             this.appId = appId;
//             this.averageRating = averageRating;
//             this.totalRatings = totalRatings;
//             this.uniqueUsers = uniqueUsers;
//         }
        
//         // Getters
//         public String getAppId() { return appId; }
//         public double getAverageRating() { return averageRating; }
//         public long getTotalRatings() { return totalRatings; }
//         public long getUniqueUsers() { return uniqueUsers; }
//     }
// }
