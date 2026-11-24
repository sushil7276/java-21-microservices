#!/usr/bin/env bash
# Crash or restart ONE service - used for the circuit breaker experiments.
#   ./scripts/service.sh crash product-service     # kill it, like a real outage
#   ./scripts/service.sh start product-service     # start it again
cd "$(dirname "$0")/.."
ROOT=$(pwd)
ACTION=${1:?usage: ./scripts/service.sh crash|start <service-name>}
NAME=${2:?usage: ./scripts/service.sh crash|start <service-name>}
case $NAME in
  discovery-server) PORT=8761;; api-gateway) PORT=8080;; user-service) PORT=8081;; product-service) PORT=8082;;
  cart-service) PORT=8083;; order-service) PORT=8084;; payment-service) PORT=8085;; shipping-service) PORT=8086;;
  favourite-service) PORT=8087;; audit-service) PORT=8088;; *) echo "unknown service $NAME"; exit 1;;
esac
case $ACTION in
  crash)
    PID=$(lsof -ti tcp:$PORT -sTCP:LISTEN) || { echo "$NAME is not running"; exit 0; }
    kill -9 $PID && echo "$NAME crashed (pid $PID)";;
  start)
    lsof -ti tcp:$PORT -sTCP:LISTEN >/dev/null && { echo "$NAME is already running"; exit 0; }
    mkdir -p logs .pids
    ( cd "$ROOT/$NAME"; nohup java -jar "target/$NAME-1.0.0.jar" > "$ROOT/logs/$NAME.out" 2>&1 & echo $! > "$ROOT/.pids/$NAME.pid" )
    echo "$NAME starting ... (wait ~30 s until http://localhost:$PORT/actuator/health is UP)";;
  *) echo "use crash or start"; exit 1;;
esac
