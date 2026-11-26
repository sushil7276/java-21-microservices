# 06 · Redis and Spring Batch – a beginner's guide

This project uses **Redis** (a very fast key-value store in memory) in four places and **Spring Batch** (a framework for big
"do this for thousands of items" jobs) in one new service, `batch-service`. This page explains what each one does,
where the code is, and how to see it working.

---

## Redis

Redis runs as the Docker container `ecommerce-redis` on port **6379** (`./scripts/start-infra.sh` starts it).
If Redis is down the shop keeps working (cache and token checks "fail open"); only carts need it.

### 1. Product cache (product-service)

**Why:** the product list is read all the time and rarely changes. Reading it from Redis saves a database query.

| | |
|---|---|
| Code | `product-service/.../config/CacheConfig.java`, `ProductService.java` |
| How | `@Cacheable` on `search` and `get`: answer from Redis if present, otherwise run the method and store the result (10 minutes). |
| Keeping it correct | `@CacheEvict` on create, update, delete, stock changes and bulk import: the cache is emptied, the next read refills it. |

```bash
curl -s localhost:8080/api/products > /dev/null                      # fills the cache
docker exec ecommerce-redis redis-cli --scan --pattern 'ecommerce:*product*'
docker exec ecommerce-redis redis-cli ttl "<a key from above>"       # seconds left
```

### 2. Shopping carts (cart-service)

**Why:** a cart is temporary data. It does not need a SQL table, and Redis is faster.

| | |
|---|---|
| Code | `cart-service/.../store/CartStore.java`, `CartLine.java`, `service/CartService.java` |
| How | One Redis **hash** per user: key `ecommerce:cart:<userId>`, field = product id, value = the line. The field `_updatedAt` says when it last changed. |
| Clean-up | The batch clean-up job deletes carts untouched for 14 days. |

```bash
docker exec ecommerce-redis redis-cli hgetall ecommerce:cart:2
```

### 3. Login revocation (user-service + api-gateway)

**Why:** a JWT is valid until it expires, so a stolen token normally cannot be cancelled. Now every user has a **version number** in Redis.

| | |
|---|---|
| Code | `user-service/.../security/TokenVersionService.java`, `JwtService.java` (claim `ver`), `api-gateway/.../RevocationCheckingJwtDecoder.java`, `user-service/.../security/TokenVersionKeys.java` (the gateway repeats the key prefix) |
| How | Login puts the current version in the token. The gateway compares it with `ecommerce:token-version:<userId>` on every request. Changing a password (or an admin resetting it, or deleting the user) raises the version, so all old tokens stop working at once. |

```bash
docker exec ecommerce-redis redis-cli get ecommerce:token-version:2
```

On the website: change your password in **Profile** → you are logged out and must sign in again.

### 4. Rate limiting (api-gateway)

**Why:** stop one client from flooding the system or guessing passwords.

| | |
|---|---|
| Code | `api-gateway/.../RateLimitConfig.java`, routes in `api-gateway/src/main/resources/application.yml` |
| How | Spring Cloud Gateway's `RequestRateLimiter` keeps a token bucket per client in Redis. `/api/auth/**` is strict (2 requests/s, burst 10, per IP); other routes allow 20/s, burst 40 per user (or per IP when not logged in). |
| Result | Too many requests → **HTTP 429** and `X-RateLimit-*` headers. |

```bash
for i in $(seq 1 15); do curl -s -o /dev/null -w "%{http_code} " -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"x","password":"y"}'; done
# 401 401 ... 429 429 429
```

---

## Spring Batch (batch-service)

A **job** is made of **steps**. A *chunk* step reads items one by one, processes them, and writes them in groups (chunks).
A *tasklet* step is just one piece of code. Spring Batch records every run in tables (`BATCH_JOB_EXECUTION` …) so you can see
what ran, when, whether it failed, and how many items were read/written/skipped.

batch-service never touches other services' databases. It calls their **internal** endpoints (`/internal/...`, not reachable through the gateway).

| Job | Type | What it does | Code |
|---|---|---|---|
| **Product import** | chunk, with *skip* | Reads a CSV, sends products to product-service in chunks. A bad line is written to a "rejected rows" table and the import continues. | `imports/` |
| **Daily sales report** | chunk (read → total) | Reads one day's orders page by page from order-service, adds up orders, items, revenue and best sellers, saves one report row. Runs every night; re-running a day replaces its report. | `report/` |
| **Clean-up** | tasklet | Deletes carts unchanged for 14 days, audit events older than 90 days and uploaded CSV files older than 7 days. Runs every night. | `cleanup/` |

Where to look in `batch-service/src/main/java/com/ecommerce/batch/`:

- `imports/`, `report/`, `cleanup/` – one folder per job: job and step definition, reader, processor, writer (start with `imports/ImportProductsJobConfig.java`; the comments walk through it).
- `client/` – calls to the other services.
- `service/JobService.java` – starts jobs **asynchronously** and returns an execution id; the website polls it.
- `schedule/ScheduledJobs.java` – the nightly runs (`@Scheduled`, time zone Asia/Kolkata).
- `web/BatchController.java` – admin-only REST endpoints (`/api/batch/...`).

### Try it

Website: sign in as admin → **Admin panel → Batch jobs**. Choose a CSV (button **CSV template** gives the format:
`name,description,price,stock,category,imageUrl`), press **Start import**, and watch the result and the rejected lines.

Or with curl (use an admin token):

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -s -X POST localhost:8080/api/batch/imports/products -H "Authorization: Bearer $TOKEN" -F file=@products.csv
curl -s -X POST "localhost:8080/api/batch/reports/sales/run?date=2026-11-25" -H "Authorization: Bearer $TOKEN"
curl -s -X POST localhost:8080/api/batch/cleanup/run -H "Authorization: Bearer $TOKEN"
curl -s "localhost:8080/api/batch/jobs?limit=10" -H "Authorization: Bearer $TOKEN"
```

To see Spring Batch's own tables: `docker exec -it ecommerce-mysql mysql -uroot -p123456 batch_db -e 'select * from BATCH_JOB_EXECUTION'`.
