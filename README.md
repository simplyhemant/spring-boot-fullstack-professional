# HRMS Attendance & Overtime Settlement Engine

This repository contains the completed hiring assignment for the Java Backend Developer role. It implements a robust, production-ready Attendance and Overtime Settlement Engine for managing a site-based blue-collar workforce, and resolves 5 critical production backend tickets.

---

## Fork Context & Setup
- **Forked Repo:** `spring-boot-fullstack-professional` (Amigoscode Spring Boot & React Course template).
- **Why:** It provides a clean, pre-configured structure with Spring Boot, Spring Data JPA, and PostgreSQL, allowing focus on the core backend implementation, database constraints, caching strategies, and connection pool tuning.

### Prerequisites
- **Java:** JDK 17 or higher
- **Database:** PostgreSQL (Supabase instance)
- **Cache:** Redis (Local or Cloud instance)

---

## Setup & Running the Application

### 1. Database Configuration (Supabase & PgBouncer)
- Create a project on [Supabase](https://supabase.com/).
- Navigate to **Project Settings > Database** to retrieve the connection details.
- **For Staging/Production:** Use the **Connection Pooler** URI on port `6543` (session/transaction mode) to connect via PgBouncer.
- Configure properties in `src/main/resources/application.properties` (or `application-staging.properties` for PgBouncer):
  ```properties
  spring.datasource.url=jdbc:postgresql://<db-host>:6543/postgres?prepareThreshold=0
  spring.datasource.username=postgres
  spring.datasource.password=<your-supabase-password>
  ```

### 2. Redis Configuration
- Ensure Redis is running locally on port `6379`.
- If using a cloud Redis instance, specify its URI and credentials in `application.properties`:
  ```properties
  spring.redis.host=localhost
  spring.redis.port=6379
  ```

### 3. Running the Server Locally
To run the Spring Boot application using Maven:
```bash
./mvnw spring-boot:run
```

To execute the test suite (backend only, skipping the frontend rebuild):
```bash
mvn test -Dskip.npm
```

---

## Part 1: API Endpoints & Curl Examples

### 1. Clock-in
Logs a worker's arrival at a site. Enforces active profile checks and prevents double clock-ins.
```bash
curl -X POST http://localhost:8080/api/attendance/clock-in \
  -H "Content-Type: application/json" \
  -d '{"workerId": 1, "siteId": 1}'
```

### 2. Clock-out
Logs departure, calculates total/overtime hours, applies shift cap calculations, and updates the active workers cache.
```bash
curl -X POST http://localhost:8080/api/attendance/clock-out \
  -H "Content-Type: application/json" \
  -d '{"workerId": 1}'
```

### 3. List Active Workers
Fetches all currently clocked-in workers. This is served exclusively from Redis for high performance.
```bash
curl -X GET http://localhost:8080/api/attendance/active
```

### 4. Paginated Attendance Logs
Gets attendance logs for a worker within a date range using optimal join fetches.
```bash
curl -X GET "http://localhost:8080/api/attendance/log?workerId=1&from=2026-05-01&to=2026-05-31&page=0&size=10"
```

### 5. Overtime Summary
Monthly overtime summary containing payout details and settlement status.
```bash
curl -X GET "http://localhost:8080/api/overtime/summary/1?month=2026-05"
```

### 6. Overtime Settlement
Marks all pending overtime entries for a worker + month as settled. This is an all-or-nothing operation.
```bash
curl -X POST "http://localhost:8080/api/overtime/settle/1?month=2026-04"
```

---

## Part 2: Resolved Production Tickets

### 1. LF-201: Configurable CORS
- **Resolution:** Added a dedicated `CorsConfig` filter registered before Spring Security processes preflight queries. Allowed origins are dynamically loaded from `app.cors.allowed-origins`.

### 2. LF-202: Resilient Redis Caching
- **Resolution:** Overrode the default `CacheErrorHandler` so that cache connection drops are caught, warnings are logged, and queries degrade gracefully to direct database fetches.

### 3. LF-203: N+1 Query Optimization
- **Resolution:** Configured lazy relationships and used JPA `JOIN FETCH` queries in `AttendanceLogRepository` to retrieve Worker and Site relations in a single database roundtrip. Added Spring Data `Pageable` support for optimized memory footprint.

### 4. LF-204: Transactional Atomicity & SMS
- **Resolution:** Extracted database mutation operations into `OvertimeSettlementTransactionHelper` annotated with `@Transactional`. Post-commit notifications are executed asynchronously using Spring's `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` to prevent SMS dispatch on transaction rollbacks.

### 5. LF-205: Connection Pool Tuning
- **Resolution:** Created `application-staging.properties` with tuned HikariCP properties (`max-lifetime=300000`, `keepalive-time=30000`) for Supabase's transaction pooler. Structured `OvertimeService` to make slow external REST calls *before* acquiring a database transaction.

---

## AI Tools & Design Decisions

### AI Tools Utilized
- **Antigravity (Google DeepMind):** Used for repository discovery, test generation, schema optimization, dependency auditing (resolving Lombok and Lettuce connection pool conflicts), and mapping transaction boundaries.

### Design Decisions & Trade-offs
- **Redis Caching Strategy:** Chose to store the active workers set in a Redis Hash. This keeps checking current workers extremely fast. Implemented TTL safety cappings of 16 hours to automatically flag open-ended shifts if a supervisor misses a clock-out.
- **Service/Transaction Partitioning:** The decision to pull the external government wage REST request out of the transaction helper prevents HikariCP connections from hanging idle, vastly increasing staging server concurrent request throughput.
- **Database level constraints:** Added composite database indices on `worker_id` and `clock_in` to guarantee rapid lookups and enforce uniqueness constraints.
