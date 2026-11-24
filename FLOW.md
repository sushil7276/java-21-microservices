# Which request goes where?

Read this together with `HELP.md`. Legend: `→` = one HTTP call, `⇒ Kafka` = event message.

## The big picture

```
 you (curl / browser)
        │  http://localhost:8080
        ▼
 ┌─────────────┐   1. checks the JWT token          ┌──────────────┐
 │ API GATEWAY │   2. asks Eureka "where is X?"  ──►│    EUREKA    │ (phone book, :8761)
 │   :8080     │   3. forwards the request          └──────────────┘
 └──────┬──────┘
        ▼
  one of the services ──► MySQL (own database)
        │
        └─ some services call other services (always by NAME, e.g. http://product-service)
```

Rules that make it easy to follow:
1. **You only ever talk to the gateway** (`:8080`). Nothing else is reachable from outside.
2. **Every service has its own database.** A service never reads another service's tables - it calls that service instead.
3. **Service → service calls use `/internal/...` URLs** (blocked at the gateway) except plain reads of `/api/products`.
4. **Every response has an `X-Trace-Id` header** - the same id appears in the logs of every service that touched the request.

## Request journeys

| You call (through the gateway) | Journey |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | gateway → **user** (login returns the JWT token) |
| `GET /api/users/me` | gateway → **user** |
| `GET /api/products`, `GET /api/products/{id}` | gateway → **product** |
| `POST/PUT/DELETE /api/products` (admin) | gateway → **product** |
| `POST /api/cart/items` | gateway → **cart** → **product** (reads the real price and stock) |
| `GET /api/cart` | gateway → **cart** |
| `POST /api/favourites/{productId}` | gateway → **favourite** → **product** (does the product exist?) |
| `GET /api/favourites` | gateway → **favourite** → **product** (once per favourite, to show details) |
| **`POST /api/orders/checkout`** | gateway → **order** → **user** → **cart** → **product** (reduce stock) → **payment** → **shipping** → **cart** (empty it); and order ⇒ Kafka ⇒ **audit** → MongoDB |
| `GET /api/orders`, `GET /api/orders/{id}` | gateway → **order** |
| `GET /api/payments` | gateway → **payment** |
| `GET /api/shipments/order/{id}` | gateway → **shipping** |
| `PUT /api/shipments/{id}/status` (admin) | gateway → **shipping** → **order** (⇒ Kafka ⇒ **audit**) |
| `GET /api/audit?entityId=...` (admin) | gateway → **audit** → MongoDB |

### The checkout, step by step (order-service/.../OrderService.java)

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
Later, the admin ships the parcel: `shipping ──► order "set SHIPPED"` ⇒ Kafka `ORDER_STATUS_CHANGED`.

## See it live

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
`<===` request arrives · `--->` answer leaves · `===>` a service calls another service · `XXX` a call failed.

For any request of your own:
```bash
curl -i http://localhost:8080/api/products | grep -i x-trace-id        # copy the id
./scripts/trace.sh <that-id>                                           # the journey, from the log files
```
Graphical version: open **Zipkin** <http://localhost:9411>, click *Run Query*, open a trace.
