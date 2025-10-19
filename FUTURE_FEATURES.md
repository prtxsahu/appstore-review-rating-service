# Future Features for Rating Stream Processing

## Performance & Scalability

1. **Bulk Database Updates** - Replace individual updates with batch operations
2. **Native DynamoDB Update Expressions** - Use atomic updates instead of fetch-then-put
3. **Configurable Flush Intervals** - Dynamic intervals based on load patterns
4. **Retry Mechanism** - Exponential backoff with circuit breaker pattern
5. **Horizontal Scaling** - Multiple stream processor instances with app partitioning

## Data Consistency & Reliability

6. **Event Ordering Guarantees** - Per-app event ordering to prevent race conditions
7. **Event Deduplication** - Detect and handle duplicate rating events
8. **Persistence Layer** - Redis for aggregations with periodic snapshots


## Advanced Features

12. **Rating Analytics** - Trends, user patterns, popularity metrics
13. **Multi-tenant Support** - Isolated rating data per tenant
14. **Configuration Management** - Runtime config updates via Spring Cloud Config

## Testing & Quality

15. **Comprehensive Testing** - Unit, integration, load, and chaos testing

## Technical Improvements

16. **Dual-Write Overhead Analysis** - Measure performance impact of DynamoDB + Elasticsearch writes
17. **Elasticsearch Data Type Consistency** - Ensure average rating is stored as double type
18. **ReentrantLock Implementation** - Replace synchronized blocks with ReentrantLock for better concurrency control
19. **Rating-Based Search Sorting** - Use app rating as score to sort search results by relevance and quality
20. **SubComment Count Management** - Implement logic to update subCommentCount when replies are added/removed
21. **Atomic Counter Operations** - Implement true atomic DynamoDB ADD operations for likes/dislikes/subCommentCount
22. **Sub-Comment API Separation** - Create separate API endpoints to fetch sub-comments (replies) for better performance and cleaner architecture
23. **Stream Processor State Recovery** - Implement @PostConstruct to fetch existing rating aggregates from DB on startup for seamless state recovery
24. **Aggregate Table Integration** - Update getAppById to fetch average rating from aggregates table instead of app table for real-time rating data
25. **User Like/Dislike Restrictions** - Implement mechanism to prevent users from liking/disliking the same comment multiple times
26. **Cascade Delete for Comments** - Implement logic to automatically delete all sub-comments when a parent comment is deleted
27. **Soft Delete vs Hard Delete** - Decide whether to use soft delete (deleted column) or hard delete for comments in DynamoDB
28. **Sub-Comment Text Aggregation** - Aggregate reply texts into top-level comments for better search relevance and UX
    - Schema: `{"comment_id": "c123", "text": "I love this feature!", "reply_texts": ["Me too!", "It crashes sometimes."], "aggregate_text": "I love this feature! Me too! It crashes sometimes.", "reply_count": 2}`
29. **Optimize Reply Queries** - Replace table scan with GSI on parentId for efficient reply retrieval OR even better if time permits move to a SQL table for comments