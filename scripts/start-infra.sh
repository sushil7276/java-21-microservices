#!/usr/bin/env bash
# Starts MySQL, MongoDB, Kafka and Zipkin in Docker and waits until they are healthy.
#   ./scripts/start-infra.sh                  everything (MySQL on localhost:3306, root / 123456)
#   ./scripts/start-infra.sh --without-mysql  you already run your own MySQL on localhost:3306 (root / 123456)
#   ./scripts/start-infra.sh --without-mongodb you already run your own MongoDB on localhost:27017 (no login)
#   ./scripts/start-infra.sh --observability  also Prometheus, Grafana and the ELK stack
set -euo pipefail
cd "$(dirname "$0")/.."
SERVICES="mysql mongodb kafka zipkin"
PROFILE=()
for arg in "$@"; do
  case $arg in
    --without-mysql) SERVICES=${SERVICES/mysql /} ;;
    --without-mongodb) SERVICES=${SERVICES/mongodb /} ;;
    --observability) SERVICES="$SERVICES prometheus grafana elasticsearch logstash kibana"; PROFILE=(--profile observability) ;;
    *) echo "unknown option $arg"; exit 1 ;;
  esac
done
if [[ $SERVICES != *mysql* ]]; then
  (echo > /dev/tcp/127.0.0.1/3306) 2>/dev/null \
    && echo "Using your own MySQL on localhost:3306 (the services log in with root / 123456)" \
    || echo "WARNING: nothing listens on localhost:3306 - start your MySQL first (user root, password 123456)"
fi
if [[ $SERVICES != *mongodb* ]]; then
  (echo > /dev/tcp/127.0.0.1/27017) 2>/dev/null \
    && echo "Using your own MongoDB on localhost:27017" \
    || echo "WARNING: nothing listens on localhost:27017 - start your MongoDB first"
fi
docker compose ${PROFILE[@]+"${PROFILE[@]}"} up -d --wait $SERVICES
docker compose ps
