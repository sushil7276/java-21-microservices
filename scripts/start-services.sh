#!/usr/bin/env bash
# Starts all Spring Boot services on this machine (background processes, logs in ./logs/*.out).
# Order: discovery-server -> api-gateway + business services.
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT=$(pwd)
mkdir -p logs .pids
source scripts/env.sh

for jar in discovery-server api-gateway user-service; do
  [[ -f $jar/target/$jar-1.0.0.jar ]] || { echo "Missing jars - run ./scripts/build.sh first"; exit 1; }
done

start() {   # name port
  local name=$1 port=$2
  local pid
  pid=$(listener_pid "$port")
  if [[ -n $pid ]]; then
    if ! is_ours "$pid"; then
      echo "  $name: port $port is used by ANOTHER program (pid $pid: $(ps -o command= -p "$pid" | cut -c1-60)) - not starting. Stop that program or free the port."
    elif running_old_jar "$name" "$pid"; then
      echo "  $name: already running but with an OLD jar (rebuilt after it started) -> ./scripts/restart-service.sh $name"
    else
      echo "  $name already running (pid $pid)"
    fi
    return
  fi
  # run from the module folder (";" not "&&": the pid saved below must be the Java process itself) so that relative paths (../logs) resolve to ./logs
  ( cd "$ROOT/$name"; nohup java -jar "target/$name-1.0.0.jar" > "$ROOT/logs/$name.out" 2>&1 & echo $! > "$ROOT/.pids/$name.pid" )
  echo "  started $name (port $port, pid $(cat .pids/$name.pid))"
}

wait_up() {  # url
  for _ in $(seq 1 90); do
    curl -fs "$1" > /dev/null 2>&1 && return 0
    sleep 2
  done
  echo "  !! $1 did not come up - check logs/*.out"; return 1
}

echo "1/2 discovery-server"
start discovery-server 8761
wait_up http://localhost:8761/actuator/health

echo "2/2 gateway + business services"
start api-gateway 8080
start user-service 8081
start product-service 8082
start cart-service 8083
start order-service 8084
start payment-service 8085
start shipping-service 8086
start favourite-service 8087
start audit-service 8088

for p in 8080 8081 8082 8083 8084 8085 8086 8087 8088; do
  wait_up http://localhost:$p/actuator/health && echo "  :$p UP"
done
# the gateway only routes to a service once it has fetched the registry (a few seconds): wait until it really does
printf "  gateway routes to the services ... "
for _ in $(seq 1 30); do
  [[ $(curl -s -o /dev/null -m 3 -w '%{http_code}' http://localhost:8080/api/products) == 200 ]] && { echo "OK"; break; }
  sleep 2
done
echo
echo "Eureka dashboard : http://localhost:8761"
echo "API gateway      : http://localhost:8080"
