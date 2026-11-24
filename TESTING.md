# Testing

The system must be running first ([START.md](START.md)); `./scripts/doctor.sh` must be green.

> **Tests create data.** The smoke test, the Postman collection and the curl examples register users, create products and place
> orders. Run them against a throw-away database, or delete the leftovers afterwards, otherwise they show up in the admin panel.

## 1. Smoke test (one command)

```bash
./scripts/smoke-test.sh          # ends with: RESULT: 48 passed, 0 failed
```
48 curl checks through the gateway, in 10 groups: security (401 without a token), user service (register, duplicate, validation, login),
product service, favourites → product, cart → product (stock limit), order + payment + delivery (checkout, stock reduced, cart emptied),
delivery lifecycle (admin ships and delivers), failed payment with compensation, authorisation between users, and the audit trail
(order → Kafka → audit-service → MongoDB).

## 2. By hand with curl

```bash
GW=http://localhost:8080
curl -s -X POST $GW/api/auth/register -H 'Content-Type: application/json' -d '{"username":"john","email":"john@mail.com","password":"secret123"}'
TOKEN=$(curl -s -X POST $GW/api/auth/login -H 'Content-Type: application/json' -d '{"username":"john","password":"secret123"}' | jq -r .token)
AUTH="Authorization: Bearer $TOKEN"

curl -s $GW/api/products | jq                                                        # public
curl -s -X POST $GW/api/cart/items -H "$AUTH" -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}' | jq
curl -s -X POST $GW/api/orders/checkout -H "$AUTH" -H 'Content-Type: application/json' \
     -d '{"shippingAddress":"12 MG Road, Pune","paymentMethod":"CARD"}' | jq           # → PAID + tracking number
```
IntelliJ: open `requests.http` and click the ▶ buttons.

| Try this | Expected |
|---|---|
| `paymentMethod: "CARD_DECLINED"` | HTTP 402, order `PAYMENT_FAILED`, stock given back |
| add more than the stock to the cart | 409 |
| `GET /api/cart` without a token | 401 |
| a normal user calls `POST /api/products` | 403 (admin only) |
| `curl $GW/internal/products/1/reduce-stock` | blocked: `/internal` URLs are never reachable from outside |
| `PUT /api/users/1` (the admin account) | 403: the admin account is protected ([ADMIN_ACCESS.md](ADMIN_ACCESS.md)) |

Admin: log in as `admin`, then `PUT /api/shipments/1/status` with `{"status":"SHIPPED"}` and later `"DELIVERED"`;
`GET /api/audit?entityId=1` shows the Kafka events of order 1. Outage experiment (circuit breaker): [ARCHITECTURE.md](ARCHITECTURE.md#5-circuit-breaker).

## 3. Postman

Import guide, variables and problems: [postman/README.md](postman/README.md). Expected result of the whole collection:
**52 requests, 79 assertions, 0 failed**. From the terminal (needs the Postman CLI, `postman --version`):

```bash
./scripts/postman-test.sh
```

## 4. The website

1. Sign up, add products to the cart, check out: the order page shows the tracking number and progress.
2. Choose the payment method **"Card that gets declined"**: the order becomes `PAYMENT FAILED`, stock is released.
3. Log in as `admin` / `admin123`: Admin panel → Products (add a product with an image link), Orders (mark the shipment shipped, then delivered),
   Users (reset a customer's password), Audit log.
4. Open the order as the customer again: the timeline has moved.
