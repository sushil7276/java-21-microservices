# Start, stop, restart and monitor

First time on this computer? Do [SETUP.md](SETUP.md) first. Ports and what each service needs: the table in [README.md](README.md#services).

Start order: **infra → discovery-server → api-gateway → the other services → website.** The scripts take care of it.

## 1. Everything

```bash
./scripts/start-infra.sh        # MySQL, MongoDB, Kafka, Zipkin (Docker must be running)
./scripts/build.sh              # only first time and after code changes
./scripts/start-services.sh     # Eureka, gateway and the 8 services (1–2 min); ends with "gateway routes ... OK"
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
./scripts/restart-service.sh product-service                 # stop, rebuild (incl. `common`), start, wait until UP
./scripts/restart-service.sh user-service product-service    # several at once
./scripts/restart-service.sh --no-build product-service      # restart only
```
`./scripts/build.sh [service ...]` only builds. `./scripts/service.sh crash <name>` kills a service with `kill -9` (for the circuit-breaker experiment).

**One service in its own IDE window, or a team that has only one service:** it needs the shared `common` library and the parent POM
from `~/.m2`. Refresh them after every change to `common`: `mvn -DskipTests -pl common -am install` (`build.sh` does this too). Details: [POM_GUIDE.md](POM_GUIDE.md).

## 3. Website

```bash
cd frontend
npm install        # first time
npm run dev        # proxies /api to the gateway on :8080
```
Gateway elsewhere: `VITE_GATEWAY_URL=http://host:8080 npm run dev`. Production build: `npm run build` (→ `dist/`).
More: [frontend/README.md](frontend/README.md).

## 4. Monitoring with Grafana (optional)

```bash
docker compose --profile observability up -d prometheus grafana
# `./scripts/start-infra.sh --observability` would also start Elasticsearch, Logstash and Kibana (needs ~6 GB for Docker)
```

| What | Where |
|---|---|
| Dashboard **E-commerce microservices overview** | <http://localhost:3000/d/ecommerce-overview> (login `admin` / `admin`; if asked for a new password: *Skip*) |
| Prometheus targets (all 10 must be **UP**) | <http://localhost:9090/targets> |

The dashboard loads automatically from `docker/grafana/provisioning/dashboards/` and shows services up, requests per second, 4xx/5xx errors,
response time, busiest and slowest endpoints, JVM memory, CPU, threads, database connections, circuit breakers and uptime. Use the
**Service** drop-down for one service, and click around the website to create traffic. Grafana re-reads the JSON every 30 s.
Stop: `docker compose --profile observability stop prometheus grafana`.

Every target `down` with `406 Not Acceptable`? Prometheus 3 and the Spring Boot 4 metrics endpoint disagree about the format.
The fix is already in `docker/prometheus/prometheus.yml` (`scrape_protocols: [PrometheusText0.0.4]`).

## 5. Scripts

| Script | Use |
|---|---|
| `doctor.sh` | what is installed, built and running, and the command that fixes each problem (only looks) |
| `start-infra.sh` | MySQL, MongoDB, Kafka, Zipkin (`--without-mysql`, `--without-mongodb`, `--observability`) |
| `build.sh [service …]` | compile; always refreshes `common` and the parent POM in `~/.m2` first |
| `start-services.sh` / `stop-services.sh` | all services on / off |
| `restart-service.sh <name> …` | stop, rebuild, start, wait until UP |
| `service.sh start\|crash <name>` | start / kill one service |
| `smoke-test.sh`, `postman-test.sh` | automatic tests ([TESTING.md](TESTING.md)) |
| `demo-trace.sh`, `trace.sh <id>` | follow one request through the services ([ARCHITECTURE.md](ARCHITECTURE.md#4-follow-a-request-live)) |

## 6. Troubleshooting

First run `./scripts/doctor.sh`. Then check the log: `tail -50 logs/<service>.out`.

| Symptom | Cause and fix |
|---|---|
| **503** or "No instances available" right after (re)starting a service | The gateway learns about a restarted service within about 10 s. Wait and retry. Start `discovery-server` first. |
| `CircuitBreakerFactory … could not be found` (or another missing class from `common`) | The service was built without `common`, so Maven used an old copy from `~/.m2`. Use `build.sh` or `restart-service.sh`, or run `mvn -DskipTests -pl common -am install`. |
| Maven `PKIX path building failed` | Company proxy; see [SETUP.md](SETUP.md#3-company-network-that-re-signs-https-pkix-path-building-failed). |
| `Port 80xx was already in use` | An old process is still alive: `./scripts/stop-services.sh`. The scripts tell you if **another program** holds the port (they never kill it). |
| A change is not visible after rebuilding | The service still runs its old jar. `start-services.sh` warns about this; run `./scripts/restart-service.sh <name>`. New database columns are added automatically (`ddl-auto: update`). |
| `Communications link failure`, Mongo or Kafka errors | Infra not running: `./scripts/start-infra.sh`, then `doctor.sh`. Container problems: [docker/README.md](docker/README.md#problems). |
| 401 even with a token | The token expired (1 hour). Log in again. |
| Audit page is empty | Kafka was not running when the order was placed. |
| Website: `npm install` hangs | Use `--registry https://registry.npmjs.org` ([SETUP.md](SETUP.md#3-company-network-that-re-signs-https-pkix-path-building-failed)). |
| Start completely clean | `./scripts/stop-services.sh && docker compose down -v`, then [SETUP.md](SETUP.md#5-first-start-step-by-step) from step 2. |

Why restarts are quick: Eureka, the gateway and the load balancer refresh every few seconds (development settings in the
`application.yml` files; keep Spring's defaults or tune them for production).
