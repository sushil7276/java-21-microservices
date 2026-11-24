#!/usr/bin/env bash
# End-to-end test of the whole architecture through the API gateway.
# Flow:  User -> Product -> Cart -> Order -> Payment -> Delivery  (+ favourites, security, audit/Kafka)
# Needs: curl, jq.   Usage: ./scripts/smoke-test.sh [gateway-url]
set -uo pipefail
GW=${1:-http://localhost:8080}
PASS=0; FAIL=0
RUN=$(date +%s)
USER_NAME="alice$RUN"

ok()   { echo "  [PASS] $1"; PASS=$((PASS+1)); }
bad()  { echo "  [FAIL] $1  -> $2"; FAIL=$((FAIL+1)); }
check() { # description expected actual
  if [[ "$2" == "$3" ]]; then ok "$1"; else bad "$1" "expected '$2' but got '$3'"; fi
}
http() { # method path [token] [json-body]  -> sets BODY and CODE
  local m=$1 p=$2 t=${3:-} d=${4:-}
  local args=(-s -o /tmp/smoke.body -w '%{http_code}' -X "$m" "$GW$p" -H 'Content-Type: application/json')
  [[ -n $t ]] && args+=(-H "Authorization: Bearer $t")
  [[ -n $d ]] && args+=(-d "$d")
  CODE=$(curl "${args[@]}"); BODY=$(cat /tmp/smoke.body)
}

echo "== 1. Security"
http GET /api/cart;                                         check "cart without token is rejected (401)" 401 "$CODE"
http GET /api/products;                                     check "product catalogue is public (200)" 200 "$CODE"

echo "== 2. User service"
http POST /api/auth/register "" "{\"username\":\"$USER_NAME\",\"email\":\"$USER_NAME@mail.com\",\"password\":\"secret123\"}"
check "register user (201)" 201 "$CODE"
http POST /api/auth/register "" "{\"username\":\"$USER_NAME\",\"email\":\"x$USER_NAME@mail.com\",\"password\":\"secret123\"}"
check "duplicate username rejected (409)" 409 "$CODE"
http POST /api/auth/register "" '{"username":"a","email":"bad","password":"1"}'
check "validation errors (400)" 400 "$CODE"
http POST /api/auth/login "" "{\"username\":\"$USER_NAME\",\"password\":\"wrong\"}"
check "wrong password rejected (400)" 400 "$CODE"
http POST /api/auth/login "" "{\"username\":\"$USER_NAME\",\"password\":\"secret123\"}"
check "login (200)" 200 "$CODE"
TOKEN=$(echo "$BODY" | jq -r .token); USER_ID=$(echo "$BODY" | jq -r .user.id)
http POST /api/auth/login "" '{"username":"admin","password":"admin123"}'
ADMIN=$(echo "$BODY" | jq -r .token)
http POST "/internal/products/1/reduce-stock?quantity=1" "$ADMIN"; check "internal endpoints are blocked at the gateway, even for admin (403)" 403 "$CODE"
http GET /api/users/me "$TOKEN";                            check "GET /users/me returns my profile" "$USER_NAME" "$(echo "$BODY" | jq -r .username)"
http GET /api/users "$TOKEN";                               check "normal user cannot list users (403)" 403 "$CODE"
http GET /api/users "$ADMIN";                               check "admin can list users (200)" 200 "$CODE"

echo "== 3. Product service"
http POST /api/products "$TOKEN" '{"name":"x","price":1,"stock":1}'
check "normal user cannot create product (403)" 403 "$CODE"
http POST /api/products "$ADMIN" "{\"name\":\"Test Phone $RUN\",\"description\":\"smoke\",\"price\":100.50,\"stock\":10,\"category\":\"Test\"}"
check "admin creates product (201)" 201 "$CODE"
PID=$(echo "$BODY" | jq -r .id)
http GET "/api/products/$PID";                              check "get product" "Test Phone $RUN" "$(echo "$BODY" | jq -r .name)"
http GET "/api/products?category=Test";                     check "filter by category" 200 "$CODE"
http GET /api/products/999999;                              check "unknown product (404)" 404 "$CODE"

echo "== 4. Favourites (favourite -> product)"
http POST "/api/favourites/$PID" "$TOKEN";                  check "add favourite (201)" 201 "$CODE"
http GET /api/favourites "$TOKEN";                          check "favourite is enriched with product data" "Test Phone $RUN" "$(echo "$BODY" | jq -r '.[0].product.name')"
http POST /api/favourites/999999 "$TOKEN";                  check "favourite for unknown product (404)" 404 "$CODE"

echo "== 5. Cart (cart -> product)"
http POST /api/cart/items "$TOKEN" "{\"productId\":$PID,\"quantity\":2}"
check "add to cart (201)" 201 "$CODE"
http POST /api/cart/items "$TOKEN" "{\"productId\":$PID,\"quantity\":1}"
check "cart total = 3 x 100.50" "301.50" "$(echo "$BODY" | jq -r '.total | tostring | if test("\\.") then . else .+".00" end' | sed -E 's/\.5$/.50/')"
http POST /api/cart/items "$TOKEN" "{\"productId\":$PID,\"quantity\":500}"
check "cannot add more than the stock (409)" 409 "$CODE"

echo "== 6. Order + Payment + Delivery (order orchestrates)"
http POST /api/orders/checkout "$TOKEN" '{"shippingAddress":"1 Main St, Pune","paymentMethod":"CARD"}'
check "checkout (201)" 201 "$CODE"
OID=$(echo "$BODY" | jq -r .id)
check "order is PAID" PAID "$(echo "$BODY" | jq -r .status)"
TRK=$(echo "$BODY" | jq -r .trackingNumber)
[[ "$TRK" == TRK-* ]] && ok "tracking number assigned ($TRK)" || bad "tracking number" "$TRK"
http GET "/api/products/$PID";                              check "stock reduced 10 -> 7" 7 "$(echo "$BODY" | jq -r .stock)"
http GET /api/cart "$TOKEN";                                check "cart emptied after checkout" 0 "$(echo "$BODY" | jq '.items | length')"
http GET "/api/payments/order/$OID" "$TOKEN";               check "payment recorded" SUCCESS "$(echo "$BODY" | jq -r '.[0].status')"
http GET "/api/shipments/order/$OID" "$TOKEN";              check "shipment created" CREATED "$(echo "$BODY" | jq -r .status)"
SID=$(echo "$BODY" | jq -r .id)
http POST /api/orders/checkout "$TOKEN" '{"shippingAddress":"x","paymentMethod":"CARD"}'
check "checkout with empty cart rejected (400)" 400 "$CODE"

echo "== 7. Delivery lifecycle (shipping -> order)"
http PUT "/api/shipments/$SID/status" "$TOKEN" '{"status":"SHIPPED"}'
check "normal user cannot update shipment (403)" 403 "$CODE"
http PUT "/api/shipments/$SID/status" "$ADMIN" '{"status":"DELIVERED"}'
check "cannot deliver before shipping (409)" 409 "$CODE"
http PUT "/api/shipments/$SID/status" "$ADMIN" '{"status":"SHIPPED"}'; check "admin ships (200)" 200 "$CODE"
http GET "/api/orders/$OID" "$TOKEN";                       check "order is SHIPPED" SHIPPED "$(echo "$BODY" | jq -r .status)"
http PUT "/api/shipments/$SID/status" "$ADMIN" '{"status":"DELIVERED"}'; check "admin delivers (200)" 200 "$CODE"
http GET "/api/orders/$OID" "$TOKEN";                       check "order is DELIVERED" DELIVERED "$(echo "$BODY" | jq -r .status)"

echo "== 8. Payment failure + compensation"
http POST /api/cart/items "$TOKEN" "{\"productId\":$PID,\"quantity\":2}" >/dev/null
http POST /api/orders/checkout "$TOKEN" '{"shippingAddress":"1 Main St","paymentMethod":"CARD_DECLINED"}'
check "declined card -> 402" 402 "$CODE"
check "order is PAYMENT_FAILED" PAYMENT_FAILED "$(echo "$BODY" | jq -r .status)"
FAILED_OID=$(echo "$BODY" | jq -r .id)
http GET "/api/products/$PID";                              check "stock restored after failed payment (still 7)" 7 "$(echo "$BODY" | jq -r .stock)"

echo "== 9. Authorisation between users"
http POST /api/auth/register "" "{\"username\":\"bob$RUN\",\"email\":\"bob$RUN@mail.com\",\"password\":\"secret123\"}" >/dev/null
http POST /api/auth/login "" "{\"username\":\"bob$RUN\",\"password\":\"secret123\"}"; BOB=$(echo "$BODY" | jq -r .token)
http GET "/api/orders/$OID" "$BOB";                         check "bob cannot read alice's order (403)" 403 "$CODE"
http GET "/api/users/$USER_ID" "$BOB";                      check "bob cannot read alice's profile (403)" 403 "$CODE"

echo "== 10. Audit trail (order -> Kafka -> audit -> MongoDB)"
sleep 3
http GET /api/audit "$TOKEN";                               check "normal user cannot read audit (403)" 403 "$CODE"
http GET "/api/audit?entityId=$OID" "$ADMIN"
check "audit events stored for the order" 200 "$CODE"
echo "     events: $(echo "$BODY" | jq -r '[.[].eventType] | join(" -> ")')"
for ev in ORDER_CREATED PAYMENT_SUCCESS SHIPMENT_CREATED ORDER_STATUS_CHANGED; do
  echo "$BODY" | jq -e --arg e $ev 'map(.eventType) | index($e)' >/dev/null && ok "audit contains $ev" || bad "audit contains $ev" "missing"
done
http GET "/api/audit?entityId=$FAILED_OID" "$ADMIN"
echo "$BODY" | jq -e 'map(.eventType) | index("PAYMENT_FAILED")' >/dev/null && ok "audit contains PAYMENT_FAILED" || bad "audit PAYMENT_FAILED" "missing"

echo
echo "RESULT: $PASS passed, $FAIL failed"
[[ $FAIL -eq 0 ]]
