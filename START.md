# START – how to run this project (all services, one service, and the frontend)

Short version: **infra → build → services → frontend**. Details and troubleshooting: [SETUP.md](SETUP.md).

| Part | Port | What it needs |
|---|---|---|
| Frontend (React) | 5173 | the API gateway |
| discovery-server (Eureka) | 8761 | nothing, start it first |
| api-gateway | 8080 | Eureka |
| user, product, cart, order, payment, shipping, favourite | 8081 – 8087 | Eureka + MySQL |
| audit-service | 8088 | Eureka + MongoDB + Kafka |
| order-service also uses | | Kafka (it publishes the audit events) |

Infra (Docker): MySQL 3306, MongoDB 27017, Kafka 9092, Zipkin 9411. Requirements: JDK 21, Maven, Docker, Node 18+.

---

## A. Start EVERYTHING

```bash
./scripts/start-infra.sh        # 1. MySQL, MongoDB, Kafka, Zipkin (Docker must be running)
./scripts/build.sh              # 2. compile (first time, or after any code change)
./scripts/start-services.sh     # 3. Eureka, gateway and the 8 services (1-2 minutes)
cd frontend && npm install && npm run dev     # 4. the website -> http://localhost:5173
```

Open <http://localhost:8761>: nine services must be **UP**. Then <http://localhost:5173>.
Quick check of the whole backend: `./scripts/smoke-test.sh`, but **do not run it on a database you care about**: it
creates test users, products and orders every time (use a throw-away database for that).

### Stop everything
```bash
./scripts/stop-services.sh      # the 10 Java services (also ones started from IntelliJ; never touches other programs)
docker compose down             # infra, data is kept   (down -v deletes ALL data)
# frontend: Ctrl+C in its terminal
```

### Next day (after a reboot)
```bash
./scripts/start-infra.sh && ./scripts/start-services.sh && (cd frontend && npm run dev)
```
No build needed unless the code changed.

---

## B. Start ONE service

Needs the infra from the table above (and Eureka for every service except `discovery-server`).

```bash
./scripts/start-infra.sh                       # once
./scripts/service.sh start discovery-server    # Eureka first
./scripts/service.sh start api-gateway         # optional, only if you use the gateway / frontend
./scripts/service.sh start user-service        # the one you work on
```
`service.sh start <name>` runs `java -jar <name>/target/<name>-1.0.0.jar` in the background, log in `logs/<name>.out`.
Wait ~30 s until `http://localhost:<port>/actuator/health` says `UP`.

Same thing by hand: `cd user-service && java -jar target/user-service-1.0.0.jar`
In IntelliJ: run the `*Application` class of that module (working directory = the module folder).

### Restart one (or more) services after changing code
```bash
./scripts/restart-service.sh product-service                  # stop, rebuild (incl. `common`), start, wait until UP
./scripts/restart-service.sh user-service product-service     # several at once
./scripts/restart-service.sh --no-build product-service       # restart only
```
A running service keeps using the jar it started with, so rebuilding alone changes nothing until it is restarted.
`./scripts/build.sh [service ...]` only builds. `service.sh crash <name>` kills one with `kill -9` (experiments only).

### Opening ONE service in its own IDE window / a team that has only one service
The service needs the shared `common` library and the parent POM. Install them once (and after every change to `common`):
```bash
mvn -DskipTests -pl common -am install
```
Details: [POM_GUIDE.md](POM_GUIDE.md).

---

## C. Start the frontend

```bash
cd frontend
npm install        # first time
npm run dev        # http://localhost:5173  (proxies /api to the gateway on :8080)
```
If `npm install` hangs, your npm registry is unreachable: `npm install --registry https://registry.npmjs.org`.
Gateway on another host: `VITE_GATEWAY_URL=http://host:8080 npm run dev`. Production build: `npm run build` (→ `dist/`).
More: [frontend/README.md](frontend/README.md).

Logins: admin `admin` / `admin123` (see [ADMIN_ACCESS.md](ADMIN_ACCESS.md)); customers sign up on the site.

---

## D. Start-up problems and how the scripts now handle them

| Problem | What was wrong | What handles it now |
|---|---|---|
| **503 right after a restart** | The gateway refreshed its service list every 30 s, Eureka cached answers for 30 s and the load balancer kept addresses for 35 s. | These are now 5 s / 3 s / 5 s (`registry-fetch-interval-seconds`, `response-cache-update-interval-ms`, `loadbalancer.cache.ttl` in the yml files). After a restart the gateway routes again about 10 s after the service is UP (it used to take about a minute). `start-services.sh` also waits until the gateway really routes. Those values are meant for development; for production keep Spring's defaults or tune them. |
| **Old `common` library broke a service** (`CircuitBreakerFactory ... could not be found`) | A single service was built without `common`, so Maven used a stale copy from `~/.m2`. | `build.sh` always rebuilds and installs `common` and the parent POM first. Use `build.sh` or `restart-service.sh`, not a bare `mvn -pl <service> package`. |
| **Maven `PKIX path building failed`** | A company proxy re-signs HTTPS and Java does not trust it. | The scripts use a truststore at `~/.m2/truststore*.jks` automatically when one exists (certificate checking stays on). Raw `mvn` calls still need `export MAVEN_OPTS="-Djavax.net.ssl.trustStore=$HOME/.m2/truststore-netskope.jks"`. |
| **Port already in use** | `stop-services.sh` only knew its own pid files, so a service started by hand or from IntelliJ kept the port. | `stop-services.sh` now stops every service of this project on the project ports. If **another program** (not this project) holds a port, the scripts tell you which one and leave it alone. |
| **Service still running an old jar** | A rebuilt jar is not used until the service restarts. | `start-services.sh` warns "running with an OLD jar" and names the fix: `./scripts/restart-service.sh <name>`. |

Other things: `Communications link failure` / Mongo or Kafka errors mean the infra is not running (`./scripts/start-infra.sh`, then
`./scripts/doctor.sh`). New database columns are added automatically (`ddl-auto: update`).
Where things are: logs `logs/<service>.out`, Eureka <http://localhost:8761>, traces Zipkin <http://localhost:9411>.
