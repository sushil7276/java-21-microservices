#!/usr/bin/env bash
# Makes one real purchase through the gateway and then prints its journey.  Needs: curl, jq
cd "$(dirname "$0")/.."
GW=http://localhost:8080
N=demo$RANDOM
j() { curl -s -H 'Content-Type: application/json' "$@"; }
j -X POST $GW/api/auth/register -d "{\"username\":\"$N\",\"email\":\"$N@mail.com\",\"password\":\"secret123\"}" > /dev/null
TOKEN=$(j -X POST $GW/api/auth/login -d "{\"username\":\"$N\",\"password\":\"secret123\"}" | jq -r .token)
j -X POST $GW/api/cart/items -H "Authorization: Bearer $TOKEN" -d '{"productId":1,"quantity":1}' > /dev/null
echo "Placing an order as $N ..."
TRACE=$(curl -s -D - -o /tmp/demo-order.json -X POST $GW/api/orders/checkout -H "Authorization: Bearer $TOKEN" \
        -H 'Content-Type: application/json' -d '{"shippingAddress":"1 Demo Street","paymentMethod":"CARD"}' \
        | grep -i '^x-trace-id' | awk '{print $2}' | tr -d '\r')
echo "Order status: $(jq -r .status /tmp/demo-order.json)    trace id: $TRACE"
sleep 2
echo
./scripts/trace.sh "$TRACE"
