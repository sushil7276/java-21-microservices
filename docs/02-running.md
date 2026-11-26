# 02 · Running – start, stop, restart, monitor, configure, troubleshoot

First time on this computer? Do [01-setup.md](01-setup.md) first. Ports and what each service needs: [README](../README.md#services).

Start order: **infra → discovery-server → api-gateway → the other services → website.** The scripts take care of it.

## 1. Everything

```bash
./scripts/start-infra.sh        # MySQL, MongoDB, Kafka, Redis, Zipkin (Docker must be running)
./scripts/build.sh              # only first time and after code changes
./scripts/start-services.sh     # Eureka, gateway and the 9 services (1–2 min); ends with "gateway routes ... OK"
cd frontend && npm run dev      # website on http://localhost:5173
```

After a reboot only `start-infra.sh`, `start-services.sh` and `npm run dev` are needed (data lives in Docker volumes).

**Stop**
```bash
./scripts/stop-services.sh      # all Java services, also ones started from IntelliJ (other programs on those ports are never touched)
docker compose down             # infra, keeps data  (`down -v` deletes ALL data)
# website: Ctrl+C
```

## 2. One service

Needs the infra, and Eureka (every service except `discovery-server`).

```bash
./scripts/service.sh start discovery-server     # Eureka first
./scripts/service.sh start api-gateway          # only if you use the website / gateway
./scripts/service.sh start user-service         # the one you work on
```
It runs `java -jar <name>/target/<name>-1.0.0.jar` in the background (log: `logs/<name>.out`); wait until
`http://localhost:<port>/actuator/health` says `UP`. In IntelliJ: run that module's `*Application` class.

**After changing code**, a running service keeps using its old jar until it is restarted:

```bash
./scripts/restart-service.sh product-service                 # stop, rebuild, start, wait until UP
./scripts/restart-service.sh user-service product-service    # several at once
./scripts/restart-service.sh --no-build product-service      # restart only
```
`./scripts/build.sh [service ...]` only builds. `./scripts/service.sh crash <name>` kills a service with `kill -9` (for the circuit-breaker experiment).

**One service in its own IDE window, or a team that has only one service:** every service is self-contained (no shared library, no root `pom.xml`).
Open its folder alone in the IDE, or build it with `cd <service> && mvn clean package`. Details: [08-maven-poms.md](08-maven-poms.md).

## 3. Website

```bash
cd frontend
npm install        # first time
npm run dev        # proxies /api to the gateway on :8080
```
Gateway elsewhere, production build and code layout: [frontend/README.md](../frontend/README.md).

## 4. Monitoring with Grafana (optional)

```bash
docker compose --profile observability up -d prometheus grafana
# `./scripts/start-infra.sh --observability` would also start Elasticsearch, Logstash and Kibana (needs ~6 GB for Docker)
```

| What | Where |
|---|---|
| Dashboard **E-commerce microservices overview** | <http://localhost:3000/d/ecommerce-overview> (login in the [README](../README.md#where-things-are); if asked for a new password: *Skip*) |
| Prometheus targets (all 11 must be **UP**) | <http://localhost:9090/targets> |

The dashboard loads automatically from `docker/grafana/provisioning/dashboards/` and shows services up, requests per second, 4xx/5xx errors,
response time, busiest and slowest endpoints, JVM memory, CPU, threads, database connections, circuit breakers and uptime. Use the
**Service** drop-down for one service, and click around the website to create traffic. Grafana re-reads the JSON every 30 s.
Stop: `docker compose --profile observability stop prometheus grafana`.

Every target `down` with `406 Not Acceptable`? Prometheus 3 and the Spring Boot 4 metrics endpoint disagree about the format.
The fix is already in `docker/prometheus/prometheus.yml` (`scrape_protocols: [PrometheusText0.0.4]`).

## 5. Scripts and configuration

| Script | Use |
|---|---|
| `doctor.sh` | what is installed, built and running, and the command that fixes each problem (only looks) |
| `start-infra.sh` | MySQL, MongoDB, Kafka, Redis, Zipkin (`--without-mysql`, `--without-mongodb`, `--observability`) |
| `build.sh [service …]` | compile (each service with its own `pom.xml`) |
| `start-services.sh` / `stop-services.sh` | all services on / off |
| `restart-service.sh <name> …` | stop, rebuild, start, wait until UP |
| `service.sh start\|crash <name>` | start / kill one service |
| `smoke-test.sh`, `postman-test.sh` | automatic tests ([03-testing.md](03-testing.md)) |
| `demo-trace.sh`, `trace.sh <id>` | follow one request through the services ([04-architecture.md](04-architecture.md#4-follow-a-request-live)) |

**Where do the values in `application.yml` come from?** A line such as `username: ${MYSQL_USER:root}` means:
*use the environment variable `MYSQL_USER` if it is set, otherwise the text after the colon (`root`)*. On your computer none of these
variables is set, so the **defaults written in each `application.yml` are what is used**; they match the Docker containers
(for example the MySQL container is created with password `123456` in `docker-compose.yml`). To use another value for one run, set the
variable when you start the service: `MYSQL_USER=bob MYSQL_PASSWORD=secret ./scripts/service.sh start shipping-service`.
To make it permanent, type the value straight into that service's `application.yml`. The variables exist so the same jar can run in Docker or
Kubernetes (`k8s/10-config.yaml`) with other settings and no code change.

**Environment variables** (all have working defaults for the Docker setup, so nothing needs to be configured):

| Variable | Default |
|---|---|
| `MYSQL_HOST` / `MYSQL_PORT` | `localhost` / `3306` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `root` / `123456` |
| `MONGO_URI` | `mongodb://localhost:27017/audit_db` |
| `KAFKA_BOOTSTRAP` | `localhost:9092` |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| `EUREKA_URL` | `http://localhost:8761/eureka` |
| `ZIPKIN_URL` | `http://localhost:9411/api/v2/spans` |
| `JWT_SECRET` | a development secret – change it ([05-security-and-admin.md](05-security-and-admin.md#4-before-using-this-project-anywhere-real)) |
| `BATCH_UPLOAD_DIR` | `./data/uploads` (batch-service, uploaded CSV files) |

If you change a port in `docker-compose.yml`, start the services with the matching variable, e.g. `MYSQL_PORT=3310 ./scripts/start-services.sh`.

## 6. Troubleshooting

First run `./scripts/doctor.sh`. Then check the log: `tail -50 logs/<service>.out`.

| Symptom | Cause and fix |
|---|---|
| **503** or "No instances available" right after (re)starting a service | The gateway learns about a restarted service within about 10 s. Wait and retry. Start `discovery-server` first. |
| **429** "Too many requests" | The gateway rate limit ([06-redis-and-batch.md](06-redis-and-batch.md#4-rate-limiting-api-gateway)). Wait a few seconds. |
| Maven `PKIX path building failed` | Company proxy: [01-setup.md](01-setup.md#3-company-network-that-re-signs-https-pkix-path-building-failed). |
| `Port 80xx was already in use` | An old process is still alive: `./scripts/stop-services.sh`. The scripts tell you if **another program** holds the port (they never kill it). |
| `port is already allocated` (3306, 27017, 6379, 9092, 9411) | Another program uses it: `lsof -i :3306` (often your own MySQL: use `--without-mysql`). Stop it, or change the left side of that port in `docker-compose.yml`. |
| A change is not visible after rebuilding | The service still runs its old jar. `start-services.sh` warns about this; run `./scripts/restart-service.sh <name>`. New database columns are added automatically (`ddl-auto: update`). |
| `Communications link failure`, Mongo, Kafka or Redis errors | Infra not running or not healthy yet: `docker ps`, then `./scripts/start-infra.sh` and `doctor.sh`. |
| `Cannot connect to the Docker daemon` | Docker is not running. Start it. |
| A container restarts again and again | Docker has too little memory. Raise it in Docker Desktop → Settings → Resources. |
| Image download fails | Network or company proxy. Retry, or check the proxy settings in Docker Desktop. |
| 401 even with a token | The token expired, or the password was changed ([05-security-and-admin.md](05-security-and-admin.md#3-sessions)). Log in again. |
| Audit page is empty | Kafka was not running when the order was placed. |
| Website: `npm install` hangs | Use `--registry https://registry.npmjs.org` ([01-setup.md](01-setup.md#3-company-network-that-re-signs-https-pkix-path-building-failed)). |
| Start completely clean | `./scripts/stop-services.sh && docker compose down -v`, then [01-setup.md](01-setup.md#4-first-start-step-by-step) from step 2. |

Why restarts are quick: Eureka, the gateway and the load balancer refresh every few seconds (development settings in the
`application.yml` files; keep Spring's defaults or tune them for production).
