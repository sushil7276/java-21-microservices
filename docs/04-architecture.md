# 04 · Architecture – how it works

Services, ports and data stores: [README](../README.md#services). Legend below: `→` one HTTP call, `⇒ Kafka` an event message.

## 1. The big picture

```
 you (browser / curl)
        │  http://localhost:8080
        ▼
 ┌─────────────┐   1. rate limit + JWT check (Redis)  ┌──────────────┐
 │ API GATEWAY │   2. asks Eureka "where is X?"  ────►│    EUREKA    │ (phone book, :8761)
 │   :8080     │   3. forwards the request            └──────────────┘
 └──────┬──────┘
        ▼
  one of the services ──► its own database
        │
        └─ some services call other services (always by NAME, e.g. http://product-service)
```

Rules that keep it easy to follow:
1. **You only talk to the gateway** (`:8080`). Nothing else is reachable from outside.
2. **Every service owns its data.** A service never reads another service's tables; it calls that service.
3. **Service → service calls use `/internal/...` URLs** (blocked at the gateway), except plain reads of `/api/products`.
4. **Every response has an `X-Trace-Id` header**; the same id is in the logs of every service that touched the request.
5. **Security:** the JWT is created by `user-service/.../JwtService.java`; who may call what is in `api-gateway/.../SecurityConfig.java` ([05-security-and-admin.md](05-security-and-admin.md)).

## 2. Request journeys

| You call (through the gateway) | Journey |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | gateway → **user** (login returns the JWT) |
| `GET /api/users/me`, `PUT /api/users/{id}` | gateway → **user** |
| `GET /api/products`, `GET /api/products/{id}` | gateway → **product** (answered from the Redis cache when possible) |
| `POST/PUT/DELETE /api/products` (admin) | gateway → **product** |
| `POST /api/cart/items` | gateway → **cart** → **product** (reads the real price and stock) |
| `GET /api/cart` | gateway → **cart** |
| `POST /api/favourites/{productId}` | gateway → **favourite** → **product** (does it exist?) |
| `GET /api/favourites` | gateway → **favourite** → **product** (once per favourite, for the details) |
| **`POST /api/orders/checkout`** | gateway → **order** → **user** → **cart** → **product** (reduce stock) → **payment** → **shipping** → **cart** (empty it); order ⇒ Kafka ⇒ **audit** → MongoDB |
| `GET /api/orders`, `GET /api/orders/{id}` | gateway → **order** |
| `GET /api/payments` | gateway → **payment** |
| `GET /api/shipments/order/{id}` | gateway → **shipping** |
| `PUT /api/shipments/{id}/status` (admin) | gateway → **shipping** → **order** (⇒ Kafka ⇒ **audit**) |
| `GET /api/audit?entityId=…` (admin) | gateway → **audit** → MongoDB |
| `POST /api/batch/…` (admin) | gateway → **batch** → internal endpoints of product / order / cart / audit ([06-redis-and-batch.md](06-redis-and-batch.md)) |

## 3. The checkout, step by step

`order-service/.../OrderService.java`:

```
 1  order ──► user      "who is user 14?"                      GET  /internal/users/14
 2  order ──► cart      "what is in his cart?"                 GET  /internal/carts/14
 3  order  (saves the order as PENDING)          ⇒ Kafka ORDER_CREATED
 4  order ──► product   "take 2 x product 1 out of stock"      POST /internal/products/1/reduce-stock
 5  order ──► payment   "charge 799.00"                        POST /internal/payments
        payment failed?  → order ──► product "give the stock back", order = PAYMENT_FAILED, HTTP 402
 6  order  (order = PAID)                         ⇒ Kafka PAYMENT_SUCCESS
 7  order ──► shipping  "create a delivery"                    POST /internal/shipments   ⇒ Kafka SHIPMENT_CREATED
 8  order ──► cart      "empty the cart"                       DELETE /internal/carts/14
```
Later the admin ships the parcel: `shipping ──► order "set SHIPPED"` ⇒ Kafka `ORDER_STATUS_CHANGED`.

## 4. Follow a request live

Every service logs a line when a request **arrives** (`<===`), when it **leaves** (`--->`) and when it **calls another service** (`===>`);
`XXX` marks a failed call. One request = one trace id in all those lines.

```bash
./scripts/demo-trace.sh                       # makes one purchase and prints its journey
```
```
18:15:16.619 [api-gateway]      <=== POST /api/orders/checkout  ==> order-service (user 14)
18:15:16.621 [order-service]    <=== POST /api/orders/checkout (user 14)
18:15:16.622 [order-service]    ===> calling user-service : get user 14
18:15:16.624 [user-service]     <=== GET /internal/users/14
18:15:16.629 [user-service]     ---> 200 GET /internal/users/14 (4 ms)
18:15:16.630 [order-service]    ===> calling cart-service : read cart of user 14
 ...
18:15:16.696 [order-service]    ---> 201 POST /api/orders/checkout (74 ms)
18:15:16.698 [api-gateway]      ---> 201 POST /api/orders/checkout (78 ms)
```

Your own request (also from Postman: copy `X-Trace-Id` from the response headers):
```bash
curl -i http://localhost:8080/api/products | grep -i x-trace-id        # copy the id
./scripts/trace.sh <that-id>                                           # its journey, from the log files
```
Graphical: Zipkin <http://localhost:9411> → *Run Query* → open a trace. Numbers over time: the Grafana dashboard ([02-running.md](02-running.md#4-monitoring-with-grafana-optional)).

## 5. Circuit breaker

**Problem:** if `product-service` is dead, every service that calls it waits and fails, one slow call after another.
**A circuit breaker is a fuse:** after enough failures it trips and rejects calls at once with a clean answer.

```
 CLOSED (normal) ──50% of the last 10 calls failed──► OPEN (reject at once, HTTP 503)
      ▲                                                     │ after 10 seconds
      └────── 2 test calls succeed ◄──── HALF_OPEN ◄────────┘      (test calls fail → back to OPEN)
```

| Where | Protects | File |
|---|---|---|
| every service → service call (cart, order, shipping, favourite, batch) | one breaker per target service | `common/.../resilience/ResilientCaller.java`, settings in `ResilienceConfig.java` |
| gateway → each service | one breaker per route, JSON 503 answer | `api-gateway/.../GatewayResilienceConfig.java`, `application.yml` (`CircuitBreaker=` filters), `FallbackController`, `GatewayErrorHandler` |

Business answers such as *404 not found* or *409 conflict* do **not** count as failures: the other service answered correctly.
The current state of every breaker is on the Grafana dashboard.

**Experiment (2 minutes)** (needs `$GW` and `$AUTH` from [03-testing.md](03-testing.md#2-by-hand-with-curl)):
```bash
./scripts/service.sh crash product-service              # simulate an outage
for i in 1 2 3 4 5 6 7 8; do curl -s -o /dev/null -w "%{http_code}\n" -X POST $GW/api/cart/items -H "$AUTH" -H 'Content-Type: application/json' -d '{"productId":1,"quantity":1}'; done
grep XXX logs/cart-service.out | tail -4                # calls 1–5: "ResourceAccessException"; from call 6: "circuit breaker is OPEN"
./scripts/service.sh start product-service              # bring it back, wait ~40 s, try again → 201 (CLOSED)
```
Every call answers **503 within milliseconds** (nothing hangs); after 5 failures the log changes from *connection refused* to
*circuit breaker is OPEN (call rejected without trying)*.

What the services are designed to do when a dependency is down (the product row and the gateway were tested live; rows marked * were not exercised):

| Dependency down | Result |
|---|---|
| product (adding to cart / favourite) | 503 "product-service is currently unavailable" |
| payment (during checkout) * | stock given back, order `CANCELLED`, 503 |
| shipping (during checkout) * | the customer already paid: order stays `PAID`, event `SHIPMENT_FAILED` |
| cart (empty-cart step) * | ignored, the order is fine |
| Kafka * | the order still works, the audit event is lost (warning in the log) |
| Redis | cache, login-revocation check and rate limiting are skipped (fail open); carts need Redis |

## 6. MapStruct (entity ⇄ JSON objects)

Database entities are never returned to the client; a **mapper** converts them. MapStruct writes the code at compile time from small interfaces:

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);        // fields with the same name are copied automatically
}
```
Mappers (folder `mapper/` in each service): `UserMapper`, `ProductMapper` (also create / update), `OrderMapper`
(cart line → order line), `PaymentMapper`, `ShipmentMapper`, `FavouriteMapper` (two sources), `AuditMapper` (Kafka event → MongoDB document).
The generated code is in `<service>/target/generated-sources/annotations/.../*MapperImpl.java`. Setup: the parent `pom.xml` has `mapstruct` and
`mapstruct-processor`, with Lombok first in the annotation-processor list ([08-maven-parent-pom.md](08-maven-parent-pom.md)).

## 7. Reading the code

Every business service has the same folders; read them in this order:

```
controller/   REST endpoints (URL → method). Thin: no logic.
service/      the business logic (OrderService is the best one to read)
repository/   database access (Spring Data, interfaces only)
entity/       database tables
dto/          JSON in / out (Java records)
mapper/       entity ⇄ dto (MapStruct)
client/       calls to OTHER services (one class per target, all use ResilientCaller)
```
cart-service stores carts in Redis (`store/`) and batch-service is organised by job (`imports/`, `report/`, `cleanup/`): see [06-redis-and-batch.md](06-redis-and-batch.md).

Shared code is in `common/`: the error format (`GlobalExceptionHandler`), `ResilientCaller` (circuit breaker), `RequestLogFilter` (the `<===` / `--->`
log lines and `X-Trace-Id`), `LoadBalancedClientConfig` (how a service finds another by name) and `common-defaults.yml` (settings shared by all services;
overridable with environment variables: [02-running.md](02-running.md#5-scripts-and-configuration)). The gateway is reactive and does **not** use `common`.
