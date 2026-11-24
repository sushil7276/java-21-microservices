# Docker setup for this project

Docker must already be installed and **running** (`docker info` must not show an error).
This guide covers only what this project needs from Docker.

## What runs in Docker

By default only the helpers run in Docker. The Spring Boot services run on your machine.

| Container | Image | Host port | Used for | Data kept in volume |
|---|---|---|---|---|
| `ecommerce-mysql` | `mysql:8.4` | **3306** | the 7 MySQL databases | `mysql-data` |
| `ecommerce-mongodb` | `mongo:8.0` | **27017** | audit database | `mongo-data` |
| `ecommerce-kafka` | `apache/kafka:3.9.0` | **9092** | audit events (topic `audit-events`) | none |
| `ecommerce-zipkin` | `openzipkin/zipkin:3` | **9411** | request tracing | none |

Optional: **Prometheus 9090 + Grafana 3000** (start them as described in [../START.md](../START.md#4-monitoring-with-grafana-optional)).
`./scripts/start-infra.sh --observability` additionally starts Elasticsearch 9200, Logstash and Kibana 5601 (about 6 GB of memory; these three were not run end-to-end).

MySQL: port **3306**, user `root`, password `123456`. MongoDB: port **27017**, no login. Both ports must be free; to use your own MySQL or
MongoDB instead of the containers see [../SETUP.md](../SETUP.md#4-free-ports). The services create their 7 databases themselves (`createDatabaseIfNotExist=true`).

Everything is defined in `docker-compose.yml` in the project root.

## Start

`./scripts/start-infra.sh` from the project root (the same without the script: `docker compose up -d --wait`).
The first time Docker downloads the images (a few minutes); afterwards it starts in seconds. The script waits until all four containers are **healthy**.

## Check

```bash
docker ps                      # 4 containers named ecommerce-*, status "healthy"
./scripts/doctor.sh            # section 3 shows OK for all four
```

## Look inside

```bash
# MySQL (user root, password 123456)
docker exec -it ecommerce-mysql mysql -uroot -p123456 -e "show databases;"
docker exec -it ecommerce-mysql mysql -uroot -p123456 -e "use order_db; select id,user_id,status,total_amount from orders;"

# MongoDB
docker exec -it ecommerce-mongodb mongosh audit_db --eval 'db.audit_logs.find().sort({receivedAt:-1}).limit(5)'

# Kafka messages
docker exec ecommerce-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic audit-events --from-beginning --max-messages 5
```
Zipkin has a web page: <http://localhost:9411>

You can also connect with a database tool (DBeaver, IntelliJ Database):
- MySQL: host `localhost`, port `3306`, user `root`, password `123456`
- MongoDB: `mongodb://localhost:27017/audit_db`

## Stop, restart, reset

| Goal | Command |
|---|---|
| Stop, keep all data | `docker compose down` |
| Start again | `./scripts/start-infra.sh` |
| Stop and delete ALL data (fresh start) | `docker compose down -v` |
| See logs of one container | `docker logs ecommerce-kafka --tail 50` |
| Restart one container | `docker restart ecommerce-mysql` |

The databases and tables are created by the services themselves on their first start.
After `docker compose down -v` the admin user and demo products are created again at the next service start.

Stop the services first (`./scripts/stop-services.sh`) when you delete the data, so they do not hold old connections.

## How the services find these containers

The services use these defaults (they point to the shifted ports), so nothing needs to be configured:

| Setting (environment variable) | Default |
|---|---|
| `MYSQL_HOST` / `MYSQL_PORT` | `localhost` / `3306` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `root` / `123456` |
| `MONGO_URI` | `mongodb://localhost:27017/audit_db` |
| `KAFKA_BOOTSTRAP` | `localhost:9092` |
| `ZIPKIN_URL` | `http://localhost:9411/api/v2/spans` |

If you change a port in `docker-compose.yml`, start the services with the matching variable, for example
`MYSQL_PORT=3310 ./scripts/start-services.sh`.

## Problems

| Problem | Fix |
|---|---|
| `Cannot connect to the Docker daemon` | Docker is not running. Start it. |
| `port is already allocated` (3306, 27017, 9092 or 9411) | Another program uses it: `lsof -i :3306` (often your own MySQL: use `--without-mysql`). Stop it or change the left side of that port in `docker-compose.yml`. |
| Services fail with `Communications link failure` | MySQL container is not running or not healthy yet: `docker ps`, then `./scripts/start-infra.sh`. |
| `ecommerce-*` containers are gone | They were removed. Run `./scripts/start-infra.sh` again (data in the volumes survives unless the volumes were also removed). |
| A container restarts again and again | Docker has too little memory. Raise it in Docker Desktop, Settings, Resources. |
| Image download fails | Network or company proxy. Retry, or check the proxy settings in Docker Desktop. |
| Audit page is empty | The Kafka container was down when the order was made. |

## Optional: run the services in Docker as well

Not needed for normal work, and not tested end-to-end (the images build, the full set of containers was not run).
```bash
./scripts/build.sh
docker compose -f docker-compose.yml -f docker-compose.apps.yml up -d --build
```
Stop the locally started services first, because the ports are the same.
