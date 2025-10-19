# HLD.md — High-Level Design

## Objective

Build a minimal, scalable Review & Rating service for an app store with:

* Keyword search across apps and comments (Elasticsearch)
* Ratings (one per user per app) and threaded text-only comments (DynamoDB)
* Near real-time average ratings using an in-service buffered aggregator (pseudo-Flink)
* No Redis, no blob storage, no external Kafka/Flink for v1

---

## Core entities

* **App**: `app_id`, `name`, `description`, `total_sum`, `total_count`, `avg_rating`, `updated_at`
* **User**: `user_id`, `name`, `created_at`
* **Rating**: composite key `(user_id, app_id)`, `value`, `created_at`, `updated_at`
* **Comment**: `comment_id`, `app_id`, `user_id`, `parent_id`, `text`, `created_at`, `updated_at`

IDs are globally unique and identical across DynamoDB and ES.

---

## Datastores

* **DynamoDB** — source of truth: tables `apps`, `ratings`, `comments`.
* **Elasticsearch / OpenSearch** — search indices: `apps_index`, `comments_index` (preview fields only).
* **No Redis, No blob storage** for this iteration.

---

## APIs (brief)

* `GET /apps?keyword=` — search apps via ES (returns app metadata + avg_rating)
* `GET /apps/{appId}` — full app + hydrated comments (DynamoDB)
* `POST /apps/{appId}/rating` — create/update rating
* `DELETE /apps/{appId}/rating` — delete rating
* `POST /apps/{appId}/comments` — add comment/reply
* `PUT /comments/{commentId}` / `DELETE /comments/{commentId}` — edit/delete comment
* `GET /comments/search?keyword=` — search comments (ES)

---

## Search behavior

* ES indexes preview fields for apps and comments (IDs, text snippets).
* `GET /apps?keyword=`: ES returns matching `app_id`s; service hydrates `avg_rating` from `apps` table.
* `GET /apps/{appId}`: full app info + comments fetched from DynamoDB (GSI on `parent_id`).
* No `score_field` for sorting in v1.

---

## Rating aggregation (in-service buffered aggregator)

* Ratings are written immediately to `ratings` table (source of truth).
* Service keeps an in-memory aggregate map per `app_id` (`sum`, `count`, `dirty`).
* On rating write/update: update in-memory aggregate.
* Periodically (configurable interval, e.g., 2–5s) flush batched aggregates to `apps` table, writing deterministic `total_sum`, `total_count`, and recomputed `avg_rating`.
* Reads return `avg_rating` from `apps` table (may be slightly stale within flush window).
* On restart, service can rebuild from `apps` + recent `ratings` or accept short staleness.
* Flushes must be idempotent: write absolute `total_sum`/`total_count` values.

---

## Comment indexing & hydration

* Comments are written to DynamoDB. An async sync job (DB stream consumer) indexes comments into ES.
* ES is used for comment text search; comment hierarchy is authoritative in DynamoDB.
* For `GET /apps/{appId}`, service queries DynamoDB for top-level comments and GSIs for replies and returns full thread.

---

## Failure & consistency notes (brief)

* Flushed aggregates are deterministic to avoid double-counting.
* Unflushed in-memory aggregates can be lost on crash — acceptable short staleness. Optionally persist small checkpoints locally.
* ES indexing is eventually consistent; direct reads from DynamoDB are authoritative.
* Support recompute of aggregates from `ratings` table for recovery/audit.

---

## Minimal implementation checklist

1. DynamoDB tables: `apps`, `ratings`, `comments`.
2. ES indices: `apps_index`, `comments_index` (preview fields).
3. API skeleton for endpoints.
4. In-service aggregator:

   * in-memory aggregate map
   * configurable flush interval
   * idempotent flush logic updating `apps` table (`total_sum`, `total_count`, `avg_rating`)
5. Comment indexing pipeline: async job to sync DynamoDB comments → ES.
6. Hydration logic for `GET /apps/{appId}` (GSI on `parent_id`).
7. Monitoring: flush lag, failed flushes, ES index lag.

---

## Future improvements

* Replace in-service buffer with Kafka + Flink for durable streaming aggregation and replayability.
* Add Redis for caching hot app pages.
* Add `score_field` and ranking in ES.
* Add blob storage for comment media.

---

## Monitoring

* Flush interval & failures
* ES index lag
* Rating write latency
* Error rates
