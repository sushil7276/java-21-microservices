# E-commerce Microservices – Beginner Guide (start, test, understand)

Java 21 · Spring Boot 4.0.10 · Spring Cloud 2025.1.5 · MySQL · MongoDB · Kafka · Resilience4j · MapStruct

New computer or forgot everything? Read **[SETUP.md](SETUP.md)** first (includes Postman).  
Three files to read: **HELP.md** (this one: start + test) · **[FLOW.md](FLOW.md)** (which request goes where) · the code (section 7).

---

## 1. What is running

| Service | Port | Job | Data |
|---|---|---|---|
| discovery-server | 8761 | Eureka: the "phone book" - services register here and find each other | – |
| api-gateway | 8080 | the ONLY entrance: checks JWT token, sends the request to the right service | – |
| user-service | 8081 | register, login (creates the JWT), profile | MySQL `user_db` |
| product-service | 8082 | product catalogue and stock | MySQL `product_db` |
| cart-service | 8083 | shopping cart | MySQL `cart_db` |
| order-service | 8084 | **the boss of a purchase** - calls user, cart, product, payment, shipping | MySQL `order_db` |
| payment-service | 8085 | pretend payment | MySQL `payment_db` |
| shipping-service | 8086 | delivery + tracking number | MySQL `shipping_db` |
| favourite-service | 8087 | wish list | MySQL `favourite_db` |
| audit-service | 8088 | listens to Kafka, stores every event | MongoDB `audit_db` |

Helpers in Docker: MySQL (3306, root / 123456), MongoDB (27017), Kafka (9092), Zipkin (9411).  
Default admin user: `admin` / `admin123`. Five demo products are created at the first start.

---

## 2. Start

```bash
./scripts/start-infra.sh        # 1. MySQL, MongoDB, Kafka, Zipkin in Docker (waits until healthy)
./scripts/build.sh              # 2. compile everything (once, and after every code change)
./scripts/start-services.sh     # 3. Eureka first, then gateway and the 8 services (1-2 minutes)
./scripts/smoke-test.sh         # 4. test: "RESULT: 48 passed, 0 failed"
```
Check: Eureka <http://localhost:8761> should list 9 services, all **UP**.  
Stop: `./scripts/stop-services.sh` and `docker compose down`.

Start one service in IntelliJ instead: run its `*Application` class (start `discovery-server` first).

---

## 3. Follow a request (tracking)

Every service writes a line when a request **arrives** (`<===`), when it **leaves** (`--->`), and when it
**calls another service** (`===>`). One request = one *trace id* shown in all those lines.

```bash
./scripts/demo-trace.sh          # buys something and prints the journey (see FLOW.md for the output)
```
Your own request:
```bash
curl -i http://localhost:8080/api/products | grep -i x-trace-id       # every response has this header
./scripts/trace.sh <trace-id>
```
Graphical: Zipkin <http://localhost:9411> → *Run Query* → click a trace.

---

## 4. Test by hand

```bash
GW=http://localhost:8080
# register + login
curl -s -X POST $GW/api/auth/register -H 'Content-Type: application/json' -d '{"username":"john","email":"john@mail.com","password":"secret123"}'
TOKEN=$(curl -s -X POST $GW/api/auth/login -H 'Content-Type: application/json' -d '{"username":"john","password":"secret123"}' | jq -r .token)
AUTH="Authorization: Bearer $TOKEN"

curl -s $GW/api/products | jq                                                       # public
curl -s -X POST $GW/api/cart/items -H "$AUTH" -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}' | jq
curl -s -X POST $GW/api/orders/checkout -H "$AUTH" -H 'Content-Type: application/json' \
     -d '{"shippingAddress":"12 MG Road, Pune","paymentMethod":"CARD"}' | jq          # -> PAID + tracking number
```
IntelliJ users: open `requests.http` and click the ▶ buttons.

| Try this | Expected |
|---|---|
| `paymentMethod: "CARD_DECLINED"` | HTTP 402, order `PAYMENT_FAILED`, stock is given back |
| add more than the stock to the cart | 409 |
| call `/api/cart` without token | 401 |
| normal user `POST /api/products` | 403 (admin only) |
| `curl $GW/internal/products/1/reduce-stock` | blocked - internal URLs are never reachable from outside |

Admin actions: login as `admin`, then `PUT /api/shipments/1/status` with `{"status":"SHIPPED"}` and later `"DELIVERED"`;
`GET /api/audit?entityId=1` shows the Kafka events of order 1.

---

## 5. Circuit breaker (it IS implemented now)

**Problem:** if `product-service` is dead, every service that calls it would wait and fail, one slow call after another.  
**Circuit breaker = a fuse.** After enough failures it "trips" and rejects calls immediately with a clean answer.

```
 CLOSED (normal) ──50% of the last 10 calls failed──► OPEN (reject at once, HTTP 503)
      ▲                                                     │ after 10 seconds
      └────── 2 test calls succeed ◄──── HALF_OPEN ◄────────┘      (test calls fail -> back to OPEN)
```

Where it is:

| Where | Protects | File |
|---|---|---|
| every service → service call (cart, order, shipping, favourite) | one breaker per target service | `common/.../resilience/ResilientCaller.java` + settings in `ResilienceConfig.java` |
| gateway → each service | one breaker per route, JSON 503 answer | `api-gateway/.../GatewayResilienceConfig.java`, `application.yml` (`CircuitBreaker=` filters), `FallbackController`, `GatewayErrorHandler` |

Business answers like *404 not found* or *409 conflict* do **not** count as failures - the other service answered correctly.

**Experiment (2 minutes):**
```bash
./scripts/service.sh crash product-service              # simulate an outage
# add to the cart 8 times - watch the answers and the log
for i in 1 2 3 4 5 6 7 8; do curl -s -o /dev/null -w "%{http_code}\n" -X POST $GW/api/cart/items -H "$AUTH" -H 'Content-Type: application/json' -d '{"productId":1,"quantity":1}'; done
grep XXX logs/cart-service.out | tail -4                 # calls 1-5: "ResourceAccessException"; from call 6: "circuit breaker is OPEN"
./scripts/service.sh start product-service              # bring it back, wait ~40 s, try again -> 201 (circuit CLOSED)
```
What you see: every call answers **503 in a few milliseconds** (nothing hangs); after 5 failures the log changes from
*connection refused* to *circuit breaker is OPEN (call rejected without trying)*.

What the services are designed to do when a dependency is down (the product row and the gateway were tested live; the rows marked * were not exercised):

| Dependency down | Result |
|---|---|
| product (while adding to cart / favourite) | 503 "product-service is currently unavailable" |
| payment (during checkout) * | stock is given back, order `CANCELLED`, 503 |
| shipping (during checkout) * | customer already paid → order stays `PAID`, event `SHIPMENT_FAILED` |
| cart (empty-cart step) * | ignored, order is fine |
| Kafka * | order should still work, audit event is lost (only a warning in the log) |

---

## 6. MapStruct (entity ⇄ JSON objects)

We never return database entities to the client. A **mapper** converts them. Before, this was hand-written
`from(...)` code; now **MapStruct** writes it at compile time from small interfaces:

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);        // same field names are copied automatically
}
```
Mappers (folder `mapper/` in each service): `UserMapper`, `ProductMapper` (also create/update), `CartMapper` (calculates `lineTotal`),
`OrderMapper` (cart line → order line), `PaymentMapper`, `ShipmentMapper`, `FavouriteMapper` (two sources),
`AuditMapper` (Kafka event → MongoDB document).  
See the generated code in `<service>/target/generated-sources/annotations/.../*MapperImpl.java`.  
Setup: `pom.xml` (parent) - `mapstruct` + `mapstruct-processor`, with Lombok first in the annotation processor list.

---

## 7. How to read the code

Every business service has the same folders - read them in this order:

```
controller/   REST endpoints (URL → method). Thin: no logic.
service/      the business logic  (OrderService is the best one to read)
repository/   database access (Spring Data - interfaces only)
entity/       database tables
dto/          JSON in / out (Java records)
mapper/       entity ⇄ dto (MapStruct)
client/       calls to OTHER services (one class per target service, all use ResilientCaller)
```
Shared code is in `common/`: error format (`GlobalExceptionHandler`), `ResilientCaller` (circuit breaker),
`RequestLogFilter` (the `<===` / `--->` log lines and `X-Trace-Id`), `LoadBalancedClientConfig` (how a service finds another by name),
`common-defaults.yml` (settings shared by all services).  
Security lives in `api-gateway/.../SecurityConfig.java` (who may call what) and `user-service/.../JwtService.java` (creates the token).

Settings can be overridden with environment variables: `MYSQL_HOST, MYSQL_PORT, MONGO_URI, KAFKA_BOOTSTRAP, EUREKA_URL, ZIPKIN_URL, JWT_SECRET`.

---

## 8. Optional: monitoring, logging, Docker, Kubernetes

* `./scripts/start-infra.sh --observability` adds Prometheus <http://localhost:9090>, Grafana <http://localhost:3000> (admin/admin), Kibana <http://localhost:5601>.
* `docker compose -f docker-compose.yml -f docker-compose.apps.yml up -d --build` runs the services as containers; `k8s/` has Kubernetes manifests.

> **Honest status:** the local setup (sections 2-6) was run and tested end-to-end. The Prometheus/Grafana/ELK containers,
> the all-in-Docker Compose file and the Kubernetes manifests are provided and syntax-checked, but **not run** here.

---

## 9. Troubleshooting

| Symptom | Fix |
|---|---|
| Maven `PKIX path building failed` | Your network re-signs HTTPS (e.g. Netskope). macOS: `export MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=KeychainStore"` then build again. Never switch certificate checking off. |
| `Communications link failure` / `Dialect` error at start | MySQL is not running: `docker compose up -d --wait mysql mongodb` (check `docker ps`: `ecommerce-mysql` must be *healthy*) |
| `port already in use` | `./scripts/stop-services.sh` (if a JVM survives: `lsof -ti tcp:8084` and kill that pid) |
| `503 ... No instances available` right after (re)starting a service | Eureka needs up to ~30 s to tell everybody. Wait and retry. Start `discovery-server` first. |
| 401 with a token | Token expired (1 h) - login again |
| Audit is empty | Kafka was not running when the order was made |
| Wipe everything | `./scripts/stop-services.sh && docker compose down -v` |

**Simplifications (demo grade):** shared JWT secret, simulated payment, `ddl-auto: update`, synchronous orchestration
with stock compensation only (a production system would add the outbox pattern / sagas, retries, mTLS).
