// package com.example.appstore.stream;

// import java.time.Instant;

// /**
//  * Observer interface for rating events.
//  * Implementations of this interface will be notified when rating changes occur.
//  */
// public interface RatingObserver {
    
//     /**
//      * Called when a new rating is created.
//      * 
//      * @param appId The application ID
//      * @param userId The user ID
//      * @param ratingValue The rating value (1-5)
//      * @param timestamp When the rating was created
//      */
//     void onRatingCreated(String appId, String userId, Integer ratingValue, Instant timestamp);
    
//     /**
//      * Called when an existing rating is updated.
//      * 
//      * @param appId The application ID
//      * @param userId The user ID
//      * @param oldRatingValue The previous rating value
//      * @param newRatingValue The new rating value
//      * @param timestamp When the rating was updated
//      */
//     void onRatingUpdated(String appId, String userId, Integer oldRatingValue, Integer newRatingValue, Instant timestamp);
    
//     /**
//      * Called when a rating is deleted.
//      * 
//      * @param appId The application ID
//      * @param userId The user ID
//      * @param deletedRatingValue The rating value that was deleted
//      * @param timestamp When the rating was deleted
//      */
//     void onRatingDeleted(String appId, String userId, Integer deletedRatingValue, Instant timestamp);
    
//     /**
//      * Called when an error occurs during rating processing.
//      * 
//      * @param appId The application ID
//      * @param userId The user ID
//      * @param error The error that occurred
//      */
//     void onRatingError(String appId, String userId, Throwable error);
// }
