#!/usr/bin/env bash
# Builds one container image per service (ecommerce/<service>:1.0.0). Run ./scripts/build.sh first.
set -euo pipefail
cd "$(dirname "$0")/.."
for m in discovery-server api-gateway user-service product-service cart-service order-service payment-service shipping-service favourite-service audit-service; do
  docker build --build-arg MODULE=$m -t ecommerce/$m:1.0.0 .
done
