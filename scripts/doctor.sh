#!/usr/bin/env bash
# "Is my machine ready, and what is running right now?"  Safe to run any time - it only looks, never changes anything.
cd "$(dirname "$0")/.."
G=$'\e[32m'; R=$'\e[31m'; Y=$'\e[33m'; N=$'\e[0m'
ok()   { echo "  ${G}OK${N}    $1"; }
bad()  { echo "  ${R}MISSING${N} $1"; }
warn() { echo "  ${Y}NOTE${N}   $1"; }

echo "1. Tools on this machine"
if command -v java >/dev/null; then
  v=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
  [[ $v -ge 21 ]] && ok "Java $v" || bad "Java 21 or newer needed (found $v)  -> install JDK 21"
else bad "Java  -> install JDK 21"; fi
command -v mvn    >/dev/null && ok "Maven $(mvn -v 2>/dev/null | head -1 | awk '{print $3}')" || bad "Maven 3.9+  -> install Maven"
command -v docker >/dev/null && ok "Docker CLI" || bad "Docker  -> install Docker Desktop"
docker info >/dev/null 2>&1 && ok "Docker is running" || bad "Docker is not running  -> start Docker Desktop"
command -v curl   >/dev/null && ok "curl" || bad "curl"
command -v jq     >/dev/null && ok "jq" || bad "jq  (needed by the test scripts)  -> brew install jq"
command -v postman >/dev/null && ok "Postman CLI (optional)" || warn "Postman CLI not installed (optional - you can use the Postman app)"

echo; echo "2. Build"
n=$(ls */target/*-1.0.0.jar 2>/dev/null | wc -l | tr -d ' ')
[[ $n -ge 10 ]] && ok "$n jars built" || bad "jars not built ($n of 10)  -> ./scripts/build.sh"

echo; echo "3. MySQL (localhost:3306, root / 123456)"
if (echo > /dev/tcp/127.0.0.1/3306) 2>/dev/null; then ok "something listens on port 3306"; else bad "nothing on port 3306  -> ./scripts/start-infra.sh  (or start your own MySQL)"; fi
if docker inspect ecommerce-mysql >/dev/null 2>&1; then
  s=$(docker inspect -f '{{.State.Status}} {{.State.Health.Status}}' ecommerce-mysql 2>/dev/null); ok "bundled container ecommerce-mysql ($s)"
  docker exec ecommerce-mysql mysql -uroot -p123456 -e "select 1" >/dev/null 2>&1 && ok "login root / 123456 works" || bad "login root / 123456 failed"
else warn "bundled MySQL container not used (fine if you run your own MySQL)"; fi
echo; echo "   MongoDB (localhost:27017)"
if (echo > /dev/tcp/127.0.0.1/27017) 2>/dev/null; then ok "something listens on port 27017"; else bad "nothing on port 27017  -> ./scripts/start-infra.sh  (or start your own MongoDB)"; fi
echo; echo "   Other containers (Kafka 9092, Redis 6379, Zipkin 9411)"
for c in ecommerce-kafka ecommerce-redis ecommerce-zipkin; do
  s=$(docker inspect -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' $c 2>/dev/null)
  [[ "$s" == running* ]] && ok "$c ($s)" || bad "$c not running  -> ./scripts/start-infra.sh"
done

echo; echo "4. Services"
for e in discovery-server:8761 api-gateway:8080 user-service:8081 product-service:8082 cart-service:8083 \
         order-service:8084 payment-service:8085 shipping-service:8086 favourite-service:8087 audit-service:8088 batch-service:8089; do
  name=${e%%:*}; port=${e##*:}
  st=$(curl -s -m 2 http://localhost:$port/actuator/health | sed -n 's/.*"status":"\([A-Z_]*\)".*/\1/p' | tail -1)
  [[ "$st" == "UP" ]] && ok "$name :$port UP" || bad "$name :$port not running  -> ./scripts/start-services.sh"
done
echo
