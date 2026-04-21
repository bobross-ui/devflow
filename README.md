# DevFlow — Developer Session Tracker API

A personal productivity REST API for tracking coding sessions, measuring time by technology or topic, setting weekly goals, and receiving summaries of coding habits. Built as a portfolio project during a job search — every feature exists because I actually needed it.

**Live API**: _(coming W4)_  
**Swagger UI**: _(coming W4)_

---

## Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.x |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Auth | JWT (JJWT 0.12.x) + Spring Security |
| Real-time | WebSocket (STOMP over SockJS) |
| Docs | springdoc-openapi 2.x (Swagger UI) |
| Containerisation | Docker + docker-compose |
| CI/CD | GitHub Actions → Render.com |

---

## Features

- **JWT Authentication** — stateless auth with access tokens (15 min) and refresh token rotation (7 days). Tokens signed with HS256, refresh token stored as SHA-256 hash — never raw.
- **Coding Session Management** — create, update, delete sessions with a strict state machine: `ACTIVE → PAUSED → ACTIVE → COMPLETED`. Invalid transitions return 409.
- **Task Checklist** — nested tasks per session with cascade delete. Completed flag per task.
- **Tag System** — many-to-many tags on sessions. Lowercase normalized. Shared across sessions — deleting a session never deletes a tag used by others.
- **Filtering & Search** — `GET /sessions` supports filtering by tag (AND logic), status, and full-text keyword search on title and notes. Composable via JPA Specifications.
- **Pagination** — all list endpoints paginated with `page`, `size`, `sortBy`, `sortDir`. Page size capped at 100.
- **Session Summary** — aggregated weekly stats: total hours this week and last week, hours by tag, hours by day (last 7 days), current streak, goal progress percentage.
- **Weekly Goals** — set a weekly coding hour target. Progress tracked against current week's completed sessions.
- **Redis Caching** — summary endpoint cached with 1hr TTL. Cache evicted on every session write — event-driven, not TTL-dependent.
- **WebSocket Timer** — real-time elapsed seconds pushed to client via STOMP over SockJS while a session is ACTIVE.
- **CSV Export** — streaming export of all sessions with date range filter. Uses `StreamingResponseBody` to avoid loading all rows into memory.
- **Webhooks** — register URLs to receive `SESSION_COMPLETED` events. Payload signed with HMAC-SHA256. Retry with exponential backoff (1s, 4s, 16s).
- **Scheduled Reports** — weekly email summary via Spring `@Scheduled`. Configurable per-user report day.
- **Rate Limiting** — token bucket algorithm. 10 req/min per IP on auth endpoints, 60 req/min per user on API endpoints. Returns 429 with `Retry-After` header.
- **Structured Error Responses** — all errors return `{status, error, message, path, timestamp}`. Consistent across controller advice and security filters.
- **Actuator + Metrics** — `/actuator/health`, `/actuator/metrics`, custom Micrometer gauge for active session count.

---

## API Endpoints

Base path: `/api/v1` — all endpoints except auth require `Authorization: Bearer <token>`

### Auth
| Method | Path | Description |
|---|---|---|
| POST | `/auth/register` | Create account |
| POST | `/auth/login` | Login, returns access + refresh token |
| POST | `/auth/refresh` | Rotate refresh token |
| POST | `/auth/logout` | Revoke refresh token |
| GET | `/auth/me` | Current user profile |

### Sessions
| Method | Path | Description |
|---|---|---|
| POST | `/sessions` | Create session (defaults to ACTIVE) |
| GET | `/sessions` | List sessions — supports `q`, `tag`, `status`, `page`, `size`, `sortBy`, `sortDir` |
| GET | `/sessions/{id}` | Get session by ID |
| PATCH | `/sessions/{id}` | Update title or notes |
| DELETE | `/sessions/{id}` | Delete session and its tasks |
| POST | `/sessions/{id}/pause` | ACTIVE → PAUSED |
| POST | `/sessions/{id}/resume` | PAUSED → ACTIVE |
| POST | `/sessions/{id}/complete` | → COMPLETED, sets duration, fires webhook |
| GET | `/sessions/summary` | Aggregated stats (Redis cached) |
| GET | `/sessions/export.csv` | Streaming CSV export |

### Tags
| Method | Path | Description |
|---|---|---|
| POST | `/sessions/{id}/tags` | Add tag to session |
| DELETE | `/sessions/{id}/tags/{tagName}` | Remove tag from session |
| GET | `/tags` | All tags across user's sessions |

### Tasks
| Method | Path | Description |
|---|---|---|
| POST | `/sessions/{id}/tasks` | Add task |
| GET | `/sessions/{id}/tasks` | List tasks |
| PATCH | `/sessions/{id}/tasks/{taskId}` | Update description or completed flag |
| DELETE | `/sessions/{id}/tasks/{taskId}` | Delete task |

### Goals
| Method | Path | Description |
|---|---|---|
| PUT | `/goals/weekly` | Set weekly hour target |
| GET | `/goals/weekly/progress` | Progress toward current week's goal |

### Webhooks
| Method | Path | Description |
|---|---|---|
| POST | `/webhooks` | Register webhook |
| GET | `/webhooks` | List webhooks |
| DELETE | `/webhooks/{id}` | Delete webhook |

---

## Running Locally

### Prerequisites
- Java 17
- PostgreSQL 16
- Redis 7 (optional until W4)

### Setup

```bash
git clone https://github.com/yourusername/devflow.git
cd devflow
```

Set environment variables:
```bash
export JWT_SECRET=your-512-bit-secret
export DB_URL=jdbc:postgresql://localhost:5432/devflow
export DB_USER=your_db_user
export DB_PASS=your_db_password
```

Run:
```bash
./mvnw spring-boot:run
```

Health check:
```
GET http://localhost:8080/actuator/health
```

### With Docker (coming W7)
```bash
docker-compose up
```

### Manual DB Setup

After first run, apply the trigram indexes:
```sql
-- src/main/resources/db/migration-notes.sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_title_trgm ON coding_session USING GIN(LOWER(title) gin_trgm_ops);
CREATE INDEX idx_notes_trgm ON coding_session USING GIN(LOWER(notes) gin_trgm_ops);
```

---

## Performance

### Full-Text Search
GIN trigram indexes on `LOWER(title)` and `LOWER(notes)` — tested on 15,000 rows:

| Metric | Before index | After index |
|---|---|---|
| Scan type | Seq Scan | Bitmap Index Scan |
| Execution time | 11.473ms | 2.902ms |
| Improvement | — | ~4x faster |

Standard B-tree indexes don't help `LIKE '%keyword%'` queries because the leading `%` prevents index range scanning. Expression-based GIN trigram indexes break the value into 3-character chunks that PostgreSQL can look up directly.

---

## Technical Trade-offs

### JWT over session-based auth
Stateless — no server-side session store needed. Any instance can validate any token without shared state, which is the foundation of horizontal scaling. The tradeoff is access tokens can't be invalidated before expiry. Mitigated with a short 15-minute expiry and refresh token rotation — a compromised access token has a small window of validity.

### Event-driven cache eviction over TTL alone
The summary endpoint includes goal progress percentage. With TTL-only caching, a user could complete a session and still see a stale percentage for up to an hour — defeating the product's purpose. `@CacheEvict` on every session write gives immediate consistency at negligible cost. TTL remains as a safety net for edge cases.

### Token bucket over sliding window rate limiting
Token bucket allows short bursts — a user can send 5 requests in quick succession as long as they have tokens. Sliding window enforces a strict requests-per-second with no burst allowance. Token bucket matches real usage patterns better — a developer hitting the API from a script may legitimately burst and then go quiet. The current implementation is in-memory per instance. At scale this moves to Redis for multi-instance consistency.

### PostgreSQL over MySQL
PostgreSQL's `pg_trgm` extension enables GIN trigram indexes for fast `ILIKE` full-text search — a core feature of `GET /sessions`. MySQL's `FULLTEXT` indexes use a different query syntax and don't support the same `ILIKE` pattern. PostgreSQL also has better support for `EXPLAIN ANALYZE` plan inspection, which was used throughout development to verify index usage.

### Offset pagination over cursor pagination
Single user with at most a few hundred sessions — offset pagination is perfectly adequate at this scale. Cursor pagination prevents the "shifting page" problem on high-write feeds (Twitter, Instagram) but adds significant implementation complexity: compound cursors, restricted sort options, no total page count. The tradeoff isn't justified here. At 10x scale with multiple users this decision would be revisited.

### JPA Specifications over custom repository methods
`GET /sessions` supports 3 independent filters in any combination — that's 8 possible filter combinations. Custom repository methods would require a method per combination. `Specification` lets each filter be a composable predicate, combined conditionally at runtime. One query method handles all combinations, and adding a new filter is a single new `Specification` method.

---

## Known Limitations

- **Rate limiter is in-memory** — works for a single instance. In a multi-instance deployment, each instance has its own token buckets and a user could exceed the global limit by routing requests across instances. Fix: move to Redis-backed rate limiting.
- **IP extraction uses `getRemoteAddr()`** — correct for direct connections. Behind a load balancer the proxy IP would be returned instead of the real client IP. Fix: read `X-Forwarded-For` from trusted proxy only, or handle at API gateway level.
- **No access token invalidation** — once issued, an access token is valid until expiry (15 min). Logout only revokes the refresh token. Fix: maintain a token blocklist in Redis keyed by JTI claim.
- **`ddl-auto: create-drop`** — database schema is dropped and recreated on every restart during development. Change to `validate` before production deployment and manage schema with Flyway.
- **Summary aggregation runs on every request** (until Redis caching in W4) — for large datasets this could be slow. Long-term fix: pre-computed materialized view refreshed on session write.
- **WebSocket timer uses `@Scheduled`** — runs on the main application thread pool. Under load this could delay ticks. Fix: dedicated scheduler thread pool or move timer to a separate service.

---

## Project Structure

```
devflow/
├── src/main/java/com/example/devflow/
│   ├── config/SecurityConfig.java
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java + JwtService.java
│   │   ├── filter/JwtAuthFilter.java + CustomUserDetails.java
│   │   └── dto/
│   ├── user/
│   ├── session/
│   │   ├── repository/SessionSpecification.java  ← JPA Specifications
│   │   └── ...
│   ├── task/
│   ├── tag/
│   ├── summary/
│   ├── webhook/
│   ├── websocket/
│   ├── scheduler/
│   ├── ratelimit/
│   │   ├── TokenBucket.java
│   │   └── RateLimitFilter.java
│   └── exception/GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration-notes.sql
├── Dockerfile                  ← coming W7
├── docker-compose.yml          ← coming W7
├── .github/workflows/ci.yml    ← coming W7
└── README.md
```

---

## Environment Variables

| Variable | Description | Required |
|---|---|---|
| `JWT_SECRET` | 512-bit base64 secret for HS256 signing | Yes |
| `DB_URL` | PostgreSQL JDBC URL | Yes |
| `DB_USER` | Database username | Yes |
| `DB_PASS` | Database password | Yes |
| `REDIS_HOST` | Redis hostname (default: localhost) | W4+ |
| `SMTP_HOST` | SMTP server for email (default: localhost) | W6+ |
| `SMTP_USER` | SMTP username | W6+ |
| `SMTP_PASS` | SMTP password | W6+ |
