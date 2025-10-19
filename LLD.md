# LLD.md — Low-Level Design (schemas & API contracts)

## DynamoDB Tables
### apps
PK: `app_id`
Attributes: `name`, `description`, `total_sum`, `total_count`, `avg_rating`, `updated_at`

### ratings
PK: `app_id`, SK: `user_id`
Attributes: `value`, `created_at`, `updated_at`, `version`

### comments
PK: `app_id`, SK: `comment_id`
Attributes: `user_id`, `parent_id`, `text`, timestamps, `deleted`
GSI: `parent_index` on `parent_id`

## Elasticsearch Indices
### apps_index
Fields: `app_id`, `name`, `description`, `keywords`, `avg_rating`, `updated_at`

### comments_index
Fields: `comment_id`, `app_id`, `parent_id`, `text`, `created_at`

## API Contracts
`GET /apps?keyword=` → returns `{ app_id, name, description, avg_rating }`
`GET /apps/{appId}` → returns full app + comments
`POST /apps/{appId}/rating` → add/update rating
`DELETE /apps/{appId}/rating` → remove rating
`POST /apps/{appId}/comments` → add comment/reply
`PUT /comments/{commentId}` / `DELETE /comments/{commentId}` → edit/delete comment
`GET /comments/search?keyword=` → comment previews from ES

## Aggregator LLD
- In-memory map per `app_id`: `{ total_sum, total_count, dirty }`
- On write/update/delete: update map, mark dirty.
- Periodic flush task writes deterministic totals to `apps` table.
- Flush idempotent, writes absolute values.
- Optional: persist checkpoint locally for recovery.

## Monitoring
- Flush interval & failures
- ES index lag
- Rating write latency
- Error rates
