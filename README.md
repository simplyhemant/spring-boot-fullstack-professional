# Construction HRMS: Attendance & Overtime Settlement Engine

This repository contains the completed backend HRMS implementation for the Java Backend Developer role. The system is designed to manage a daily wage, shift-based, overtime-heavy blue-collar workforce (such as site supervisors, crews, and payroll operators) and is fully optimized for scale, data integrity, and resilience.

---

## 🛠️ Tech Stack & Key Features
- **Framework**: Spring Boot (v2.4.3), Java 17+ (tested on Java 21)
- **Database**: PostgreSQL (hosted on Supabase)
- **Caching**: Redis (caching active workers, custom resilience error handler)
- **Connection Pool**: HikariCP (optimized settings for transaction-pooler PgBouncer)
- **API Testing**: Postman Collection committed directly in the project root (`HRMS_Postman_Collection.json`)

---

## 🚀 Setup & Installation

### 1. Database Configuration
By default, the application is configured to connect to PostgreSQL.
For local PostgreSQL development, update the database details in `src/main/resources/application.properties`.
For production-like connection pooling (using Supabase with PgBouncer at port `6543`), configure the settings in `src/main/resources/application-staging.properties`:
```properties
spring.datasource.url=jdbc:postgresql://<db-host>:6543/postgres?prepareThreshold=0
spring.datasource.username=postgres
spring.datasource.password=<your-password>
```
> [!IMPORTANT]
> The parameter `prepareThreshold=0` is critical when connecting to PgBouncer in transaction mode to prevent prepared statement errors.

### 2. Redis Configuration
Make sure Redis is running on port `6379`.
```properties
spring.redis.host=localhost
spring.redis.port=6379
```

### 3. Build and Run
To run the Spring Boot server locally:
```bash
mvn spring-boot:run -P"!build-frontend"
```

To run the unit and integration tests:
```bash
mvn test -P"!build-frontend"
```

---

## 📦 Database Seeding & Setup APIs
To make testing convenient, two endpoints are exposed to seed workers and sites into the database before clocking in.

### 1. Create a Worker (Seed)
* **Endpoint**: `POST /api/attendance/workers`
* **Request Body**:
```json
{
  "name": "John Doe",
  "phone": "9876543210",
  "designation": "MASON",
  "dailyWageRate": 800.0,
  "active": true
}
```

### 2. Create a Site (Seed)
* **Endpoint**: `POST /api/attendance/sites`
* **Request Body**:
```json
{
  "siteName": "Metro Tunnel Site A",
  "location": "Sector 62, Noida",
  "active": true
}
```

---

## ⏱️ Custom-Time Testing & Overtime Simulation
Since real-time clock-in/out calls only span a few seconds (producing `0.0` overtime), the clock-in and clock-out endpoints support optional **custom timestamps** (`ISO DATE_TIME` format) to easily simulate historical shifts.

### Step 1: Clock-In at a past time (e.g. 11.5 hours ago in May)
* **Endpoint**: `POST /api/attendance/clock-in`
* **Request Body**:
```json
{
  "workerId": 1,
  "siteId": 1,
  "clockInTime": "2026-05-15T08:00:00"
}
```

### Step 2: Clock-Out with a custom time
* **Endpoint**: `POST /api/attendance/clock-out`
* **Request Body**:
```json
{
  "workerId": 1,
  "clockOutTime": "2026-05-15T19:30:00"
}
```
* **Result**: Generates a shift of `11.5` hours, producing `3.5` hours of overtime (`11.5` total hours - `8.0` standard hours).

### Step 3: Fetch Monthly Overtime Summary
* **Endpoint**: `GET /api/overtime/summary?workerId=1&month=2026-05`
* **Response**:
```json
{
  "workerId": 1,
  "workerName": "John Doe",
  "month": "2026-05",
  "totalOvertimeHours": 3.5,
  "totalPayout": 600.0,
  "settlementStatus": "PENDING",
  "details": [
    {
      "date": "2026-05-15",
      "overtimeHours": 3.5,
      "rateApplied": 150.0,
      "amount": 600.0,
      "settlementStatus": "PENDING"
    }
  ]
}
```

### Step 4: Settle Monthly Overtime
* **Endpoint**: `POST /api/overtime/settle?workerId=1&month=2026-05`
* **Result**: Settles all pending entries for that month atomically and triggers an asynchronous SMS confirmation.
> [!NOTE]
> Settlement is restricted to past completed months (you cannot settle the current or a future month).

---

## 🛠️ Core API Reference

| Endpoint | Method | Params / Body | Description |
|---|---|---|---|
| `/api/attendance/clock-in` | `POST` | `{"workerId": 1, "siteId": 1, "clockInTime": "..."}` | Clock-in a worker. Saves active worker state to Redis. |
| `/api/attendance/clock-out` | `POST` | `{"workerId": 1, "clockOutTime": "..."}` | Clock-out a worker. Computes hours, overtime, and removes from Redis. |
| `/api/attendance/active` | `GET` | None | Fetches all currently clocked-in workers directly from Redis. |
| `/api/attendance/logs` | `GET` | `workerId`, `from`, `to`, `page`, `size` | Paginated retrieval of attendance records with JOIN FETCH optimizations. |
| `/api/overtime/summary` | `GET` | `workerId`, `month` (`YYYY-MM`) | Fetches total monthly overtime, rates, payouts, and settlement status. |
| `/api/overtime/settle` | `POST` | `workerId`, `month` (`YYYY-MM`) | Settles pending overtime hours atomically. |

---

## 🧾 Solved Production Tickets

### 🔍 LF-201: Configurable CORS Filter
* **Problem**: Staging frontends on different domains failed due to missing/misconfigured CORS headers.
* **Solution**: Registered a custom `CorsFilter` bean mapping allowed origins dynamically loaded from `app.cors.allowed-origins`.

### 🛡️ LF-202: Resilient Redis Caching
* **Problem**: Redis server downtime crashed the entire clock-in/out request lifecycle.
* **Solution**: Overrode `CacheErrorHandler` so that cache connection exceptions are caught, logged, and queries degrade gracefully to standard PostgreSQL database lookups instead of crashing.

### ⚡ LF-203: N+1 Query Optimization
* **Problem**: Clock-in and log-retrieval APIs were making hundreds of database calls due to lazy loading.
* **Solution**: Utilized JPQL `JOIN FETCH` queries in `AttendanceLogRepository` to retrieve worker and site entities in a single database roundtrip. Rewrote dynamic queries with `COALESCE` to solve PostgreSQL parameter type-inference errors.

### 🔒 LF-204: Transactional Atomicity & Post-Commit Actions
* **Problem**: Monthly settlements are financial operations and must be all-or-nothing. However, failures in SMS dispatch caused entire settlements to rollback, or rolled-back settlements still fired SMS notifications.
* **Solution**: Separated transactional mutations into a dedicated helper. Leveraged `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` with an asynchronous executor (`@Async`) to ensure SMS alerts only dispatch *after* a successful transaction commit.

### 🎛️ LF-205: Connection Pool Tuning
* **Problem**: Supabase's transaction pooler (PgBouncer) closed idle connections frequently, causing application-side timeouts. Slow external REST calls held active database connections open, starving the connection pool.
* **Solution**: Added tuned HikariCP settings (`max-lifetime=300000`, `keepalive-time=30000`) matching PgBouncer's profile. Restructured the service to make slow external wage multiplier REST calls *before* acquiring a database transaction.

---

## 🏛️ Architectural Considerations: Business & Humans
* **The Site Supervisor**: Standing on a hot construction site, the supervisor needs to clock in 40+ workers in minutes. The active worker list is cached in a Redis set to guarantee sub-millisecond response times.
* **The Payroll Operator**: At month-end, payroll needs exact numbers. Real people's livelihoods depend on these wages. We enforce database-level index constraints and transactional atomicity to prevent duplicate, partial, or corrupt settlement figures.
* **The Worker**: Trust is built on transparency. The worker receives an automated, post-commit SMS specifying their settled hours and wage. If a supervisor forgets to clock out a worker, the system automatically flags any shift exceeding 16 hours for review.
