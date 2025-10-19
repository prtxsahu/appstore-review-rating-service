# App Store Review & Rating Service Implementation

## Overview

Build a minimal, scalable Review & Rating service with Elasticsearch for search, DynamoDB for persistence, and an in-service aggregator for near real-time rating calculations.

## Architecture Changes from Original Design

1. **New tables**: Add `users`, `aggregates` tables to DynamoDB
2. **Updated `apps` schema**: Remove `total_sum`, `total_count` (moved to `aggregates` table)
3. **Simplified fields**: Remove `version` from ratings, `deleted` from comments
4. **Comment fetching**: Single-level fetch by default; separate API for nested replies

## Implementation Steps

### 1. Project Setup & Dependencies

- Add to `pom.xml`: AWS SDK v2 (DynamoDB), OpenSearch Java client, Spring Boot actuator
- Configure `application.properties` for LocalStack DynamoDB + local Elasticsearch
- Add Docker Compose file for LocalStack and Elasticsearch containers

### 2. DynamoDB Schema Implementation

**Tables to create:**

- `users`: PK=`user_id`, attributes: `name`, `created_at`
- `apps`: PK=`app_id`, attributes: `name`, `description`, `avg_rating`, `updated_at`
- `ratings`: PK=`app_id`, SK=`user_id`, attributes: `value`, `created_at`, `updated_at`
- `comments`: PK=`app_id`, SK=`comment_id`, GSI on `parent_id`, attributes: `user_id`, `parent_id`, `text`, `created_at`, `updated_at`
- `aggregates`: PK=`app_id`, attributes: `total_sum`, `total_count`, `last_updated`

Create repository layer with DynamoDB Enhanced Client for each entity.

### 3. Elasticsearch Indices

**Indices:**

- `apps_index`: Fields: `app_id`, `name`, `description`, `keywords`, `avg_rating`, `updated_at`
- `comments_index`: Fields: `comment_id`, `app_id`, `parent_id`, `text`, `created_at`

Create ES repository/service layer for indexing and search operations.

### 4. Core Domain Models & DTOs

- Entity classes: `User`, `App`, `Rating`, `Comment`, `Aggregate`
- Request DTOs: `CreateUserRequest`, `CreateAppRequest`, `RatingRequest`, `CommentRequest`
- Response DTOs: `AppResponse`, `CommentResponse`, `SearchResponse` with pagination support

### 5. In-Memory Rating Aggregator

Create `RatingAggregatorService` with:

- `ConcurrentHashMap<String, AggregateData>` for in-memory state (`app_id` → `{sum, count, dirty}`)
- Synchronized methods for rating write/update/delete
- Scheduled task (configurable interval, e.g., 5s) to flush dirty aggregates to `aggregates` table
- Idempotent flush: write absolute `total_sum`/`total_count`, recompute and update `avg_rating` in `apps` table
- On startup: rebuild in-memory map from `aggregates` table
- Graceful shutdown: flush all pending aggregates

### 6. REST API Controllers

**UserController:**

- `POST /users` - Create user (persist to DynamoDB only)

**AppController:**

- `POST /apps` - Create app (persist to DynamoDB + sync to ES immediately)
- `GET /apps?keyword={keyword}&page={page}&size={size}` - Search apps via ES, return with `avg_rating` from `apps` table
- `GET /apps/{appId}` - Get full app + top-level comments only (no nested replies)

**RatingController:**

- `POST /apps/{appId}/rating` - Create/update rating (validate user exists, update aggregator, persist to DynamoDB)
- `DELETE /apps/{appId}/rating` - Delete rating (update aggregator, remove from DynamoDB)

**CommentController:**

- `POST /apps/{appId}/comments` - Add comment/reply (persist to DynamoDB + sync to ES immediately)
- `PUT /comments/{commentId}` - Edit comment (update DynamoDB + ES)
- `DELETE /comments/{commentId}` - Delete comment (remove from DynamoDB + ES)
- `GET /comments/{commentId}/replies?page={page}&size={size}` - Fetch direct child comments
- `GET /comments/search?keyword={keyword}&page={page}&size={size}` - Search comments via ES

### 7. Service Layer Logic

**Dual-write strategy**: For operations requiring both DynamoDB and ES:

1. Write to DynamoDB first (source of truth)
2. Immediately sync to ES in same request (blocking)
3. If ES fails, log error but continue (eventual consistency acceptable)

**Rating flow:**

1. Validate user and app exist
2. Check if rating exists (update vs create)
3. Write to `ratings` table
4. Update in-memory aggregator (marks `app_id` as dirty)
5. Background flush task periodically writes to `aggregates` table and updates `avg_rating` in `apps` table + ES

**Comment hydration:**

- `GET /apps/{appId}`: Query comments with `parent_id=null` only (top-level)
- `GET /comments/{commentId}/replies`: Query GSI `parent_index` for specific parent

### 8. Configuration & Infrastructure

- Create `DynamoDBConfig` for Enhanced Client setup (LocalStack endpoint)
- Create `ElasticsearchConfig` for OpenSearch client (local endpoint)
- Add scheduled task configuration for aggregator flush interval
- Docker Compose with LocalStack and Elasticsearch 8.x

### 9. Exception Handling & Validation

- Global exception handler for consistent error responses
- Input validation for all DTOs (JSR-303)
- Custom exceptions: `ResourceNotFoundException`, `DuplicateResourceException`, `InvalidRatingException`

### 10. Monitoring & Health Checks

- Actuator endpoints for health checks
- Custom metrics: flush lag, failed flushes, ES sync failures
- Logging for aggregator flush operations and ES sync status

## Key Files to Create/Modify

**Configuration:**

- `application.properties` - LocalStack/ES endpoints, aggregator flush interval
- `docker-compose.yml` - LocalStack + Elasticsearch containers
- `pom.xml` - Add AWS SDK v2, OpenSearch client dependencies

**Domain/Entities:**

- `User.java`, `App.java`, `Rating.java`, `Comment.java`, `Aggregate.java`

**Repositories:**

- `UserRepository.java`, `AppRepository.java`, `RatingRepository.java`, `CommentRepository.java`, `AggregateRepository.java`
- `AppSearchRepository.java`, `CommentSearchRepository.java`

**Services:**

- `RatingAggregatorService.java` - Core aggregation logic
- `AppService.java`, `RatingService.java`, `CommentService.java`, `UserService.java`

**Controllers:**

- `UserController.java`, `AppController.java`, `RatingController.java`, `CommentController.java`

## Testing Strategy

- Unit tests for aggregator logic (mock DynamoDB)
- Integration tests with Testcontainers (DynamoDB + ES)
- Test aggregator recovery on restart
- Test dual-write failure scenarios

## Implementation Progress

### Completed ✅
- [x] Add AWS SDK v2, OpenSearch client, and other dependencies to pom.xml
- [x] Create docker-compose.yml for LocalStack and Elasticsearch (with data persistence)
- [x] Configure application.properties for LocalStack DynamoDB and local Elasticsearch endpoints
- [x] Create DynamoDBConfig and ElasticsearchConfig beans (fixed linting errors)
- [x] Fix Java version compatibility issue (changed from Java 21 to Java 17)
- [x] Resolve all Maven compilation errors
- [x] Create production-ready OpenSearch client configuration with proper authentication
- [x] Update OpenSearch dependencies to latest stable versions (2.9.0)
- [x] Create domain entities: User, App, Rating, Comment, Aggregate with DynamoDB annotations
- [x] Create request/response DTOs with validation annotations
- [x] Create controller classes with dummy responses (properly separated, ready for service layer integration)
- [x] Add app-specific comment search controller for scoped comment search
- [x] Implement DynamoDB repositories for all entities using Enhanced Client (only essential methods, organized in dynamodb subdirectory)
- [x] Implement Elasticsearch repositories for apps_index and comments_index (organized in elasticsearch subdirectory)
- [x] Create UserService and AppService with DynamoDB persistence (basic CRUD operations)
- [x] Connect UserController and AppController to service layer
- [x] Add comprehensive logging to all flows (Controller → Service → Repository)
- [x] Create initialization services for DynamoDB tables and Elasticsearch indices

### In Progress 🔄
- [ ] Build RatingAggregatorService with in-memory map, scheduled flush, and startup recovery logic

### Pending ⏳
- [ ] Build RatingAggregatorService with in-memory map, scheduled flush, and startup recovery logic
- [ ] Create UserService and UserController for POST /users endpoint
- [ ] Create AppService and AppController with dual-write to DynamoDB + ES
- [ ] Create RatingService and RatingController integrating with aggregator
- [ ] Create CommentService and CommentController with dual-write and pagination
- [ ] Implement search endpoints for apps and comments with pagination
- [ ] Create global exception handler and custom exceptions
- [ ] Add actuator configuration and custom metrics for aggregator and ES sync
- [ ] Create initialization script/service to create DynamoDB tables and ES indices on startup

## Notes

- All operations use dual-write strategy: DynamoDB first (source of truth), then ES sync
- Aggregator persists to `aggregates` table periodically and rebuilds on startup
- Comment threading: fetch only one level by default, use separate API for nested replies
- No authentication required for v1
- Pagination supported for all search and list endpoints
