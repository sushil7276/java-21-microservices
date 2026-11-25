# 07 · Infrastructure – the Docker containers

Docker must already be installed and **running** (`docker info` must not show an error). The Spring Boot services run on your machine;
only the helpers below run in Docker. Everything is defined in `docker-compose.yml` in the project root.
Problems with containers or ports: [02-running.md](02-running.md#6-troubleshooting).

## What runs in Docker

| Container | Image | Host port | Used for | Data kept in volume |
|---|---|---|---|---|
| `ecommerce-mysql` | `mysql:8.4` | **3306** | the 7 MySQL databases (user, product, order, payment, shipping, favourite, batch) | `mysql-data` |
| `ecommerce-mongodb` | `mongo:8.0` | **27017** | audit database | `mongo-data` |
| `ecommerce-kafka` | `apache/kafka:3.9.0` | **9092** | audit events (topic `audit-events`) | none |
| `ecommerce-redis` | `redis:7.4-alpine` | **6379** | product cache, carts, token versions, rate limits ([06](06-redis-and-batch.md)) | `redis-data` |
| `ecommerce-zipkin` | `openzipkin/zipkin:3` | **9411** | request tracing | none |

Optional (start: [02-running.md](02-running.md#4-monitoring-with-grafana-optional)): **Prometheus 9090 + Grafana 3000**.
`./scripts/start-infra.sh --observability` additionally starts Elasticsearch 9200, Logstash and Kibana 5601 (about 6 GB of memory; these three were not run end-to-end).

MySQL login and the other credentials: [README](../README.md#where-things-are). MongoDB has no login. The services create their databases and tables
themselves (`createDatabaseIfNotExist=true`, `ddl-auto: update`).

## Start and check

`./scripts/start-infra.sh` from the project root (without the script: `docker compose up -d --wait`). The first time Docker downloads the
images (a few minutes); afterwards it starts in seconds. The script waits until all containers are **healthy**.

```bash
docker ps                      # containers named ecommerce-*, status "healthy"
./scripts/doctor.sh            # section 3 shows OK for each
```

## Look inside

```bash
# MySQL
docker exec -it ecommerce-mysql mysql -uroot -p123456 -e "show databases;"
docker exec -it ecommerce-mysql mysql -uroot -p123456 -e "use order_db; select id,user_id,status,total_amount from orders;"

# MongoDB
docker exec -it ecommerce-mongodb mongosh audit_db --eval 'db.audit_logs.find().sort({receivedAt:-1}).limit(5)'

# Kafka messages
docker exec ecommerce-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic audit-events --from-beginning --max-messages 5

# Redis (more commands: 06-redis-and-batch.md)
docker exec ecommerce-redis redis-cli --scan --pattern 'ecommerce:*'
```
Zipkin has a web page: <http://localhost:9411>. With a database tool (DBeaver, IntelliJ Database): MySQL `localhost:3306`,
MongoDB `mongodb://localhost:27017/audit_db`, Redis `localhost:6379`.

## Stop, restart, reset

Stop the services first (`./scripts/stop-services.sh`) when you delete data, so they do not hold old connections.

| Goal | Command |
|---|---|
| Stop, keep all data | `docker compose down` |
| Start again | `./scripts/start-infra.sh` |
| Stop and delete ALL data (fresh start) | `docker compose down -v` |
| See logs of one container | `docker logs ecommerce-kafka --tail 50` |
| Restart one container | `docker restart ecommerce-mysql` |

After `docker compose down -v` the admin user and demo products are created again at the next service start.

## Use your own MySQL or MongoDB

If your own MySQL or MongoDB already uses 3306 / 27017, use it instead of the container (MySQL must accept `root` / `123456`; MongoDB needs no login):

```bash
./scripts/start-infra.sh --without-mysql        # and/or --without-mongodb
```

## Optional: run the services in Docker as well

Not needed for normal work, and not tested end-to-end (the images build, the full set of containers was not run; batch-service and Redis settings are not in this file yet).
```bash
./scripts/build.sh
docker compose -f docker-compose.yml -f docker-compose.apps.yml up -d --build
```
Stop the locally started services first, because the ports are the same.
