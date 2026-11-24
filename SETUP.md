# SETUP – "I forgot this project" / "I cloned it on a new computer"

Start here. Everything you need to get from zero to a working system, and to test it in Postman.

---

## 0. The 30-second version

```bash
./scripts/doctor.sh             # what is missing / what is running?  (safe, only looks)
./scripts/start-infra.sh        # MySQL + MongoDB + Kafka + Zipkin in Docker
./scripts/build.sh              # compile (only needed the first time and after code changes)
./scripts/start-services.sh     # Eureka, gateway, 8 services  (1-2 minutes)
./scripts/smoke-test.sh         # automatic test -> "RESULT: 48 passed, 0 failed"
```
Then open Postman (section 5). When finished: `./scripts/stop-services.sh` and `docker compose down`.

**Forgot what is going on?** Run `./scripts/doctor.sh` - it tells you what is installed, built and running, and which command fixes each problem.

---

## 1. What this project is (one minute)

An online shop split into small programs (microservices). You only talk to the **gateway** on port 8080.

```
 Postman / curl ──► API GATEWAY :8080 ──► user, product, cart, order, payment, shipping, favourite, audit
                         │                    (each has its own database; order-service is the "boss" of a purchase)
                         └── EUREKA :8761 (phone book: services find each other by name)
```
More: **FLOW.md** (which request goes where) · **HELP.md** (circuit breaker, MapStruct, how to read the code).

---

## 2. New computer – install once

| Tool | Version | Check | Install (macOS) |
|---|---|---|---|
| Java JDK | **21** | `java -version` | `brew install openjdk@21` |
| Maven | 3.9+ | `mvn -v` | `brew install maven` |
| Docker Desktop | any recent | `docker info` (must not error) | <https://www.docker.com/products/docker-desktop> – **start it** |
| jq | any | `jq --version` | `brew install jq` |
| curl | any | `curl --version` | already installed |
| Postman | app **or** CLI | – | <https://www.postman.com/downloads/> |
| IDE (optional) | IntelliJ IDEA | – | open the folder as a Maven project |

Linux: `sudo apt install openjdk-21-jdk maven jq curl` + Docker Engine.  
**Windows:** the helper scripts are bash scripts. Use **WSL2** (Ubuntu) with Docker Desktop's WSL integration, or Git Bash.
*(I only tested this on macOS.)* Without bash you can still run the apps from IntelliJ (section 7) and use Postman.

### Get the code
```bash
git clone <your-repository-url> Microservices
cd Microservices
```
> The project folder on the original machine is **not a git repository yet**. Do this once there, so cloning works later:
> ```bash
> cd /Users/subabar/IdeaProjects/Microservices
> git init && git add . && git commit -m "E-commerce microservices"
> git remote add origin <your-repository-url> && git push -u origin main
> ```
> `.gitignore` already excludes `target/`, `logs/`, `.pids/`.

### Company network that re-signs HTTPS (error `PKIX path building failed`)
Maven cannot download libraries because Java does not trust your company's certificate. On macOS tell Java to use the system keychain:
```bash
export MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=KeychainStore"      # then run ./scripts/build.sh again
```
The scripts (`build.sh`, `restart-service.sh`) also use a truststore at `~/.m2/truststore*.jks` automatically if you have one. Do **not** disable certificate checking. (Linux/Windows: import the company root certificate into the JDK `cacerts`.)

### Free ports
`8080-8088, 8761` (services) and `3306 (MySQL), 27017 (MongoDB), 9092, 9411` (Docker).
**Port 3306 must be free**: if a MySQL is already running there (your own, or another container), either stop it, or use it: it must accept user `root` / password `123456`, then start with `./scripts/start-infra.sh --without-mysql`.
The same for **port 27017**: use your own local MongoDB (no login needed) with `--without-mongodb`. Both flags can be combined. Postgres does not conflict.

---

## 3. First start – step by step

| # | Command | What happens | You should see |
|---|---|---|---|
| 1 | `./scripts/doctor.sh` | checks tools | no red `MISSING` in section 1 |
| 2 | `./scripts/start-infra.sh` | pulls images (first time ~2-5 min) and starts 4 containers | 4 containers `healthy` |
| 3 | `./scripts/build.sh` | downloads libraries, compiles 11 modules (first time ~3-5 min) | `BUILD SUCCESS` |
| 4 | `./scripts/start-services.sh` | starts Eureka, then everything else | `:8080 UP` … `:8088 UP` |
| 5 | `./scripts/doctor.sh` | everything green | sections 3 and 4 all `OK` |
| 6 | `./scripts/smoke-test.sh` | 48 automatic checks | `RESULT: 48 passed, 0 failed` |

Open <http://localhost:8761> – nine services listed, all **UP**.

The databases and tables are created automatically. The admin user (`admin` / `admin123`) and ten demo products (with photos, prices in rupees) are created at the first start when the product table is empty. How to start the services and the frontend: [START.md](START.md).

---

## 4. Every other day (after a reboot, or when you come back)

Data is kept in Docker volumes, so nothing is lost.
```bash
./scripts/start-infra.sh        # Docker must be running
./scripts/start-services.sh     # (no build needed unless you changed code)
```
Done working:
```bash
./scripts/stop-services.sh
docker compose down             # keeps data      |  docker compose down -v  deletes all data
```
Changed code? → `./scripts/stop-services.sh && ./scripts/build.sh && ./scripts/start-services.sh`  
Restarted only one service? → wait ~30 s before testing (Eureka needs time to tell the others).

---

## 5. Test with Postman

Full import guide: **[postman/README.md](postman/README.md)**.

Files (folder `postman/`):
* `Ecommerce-Microservices.postman_collection.json` – 52 requests in 10 folders, with automatic checks
* `Ecommerce-Local.postman_environment.json` – variables (`baseUrl`, tokens, ids)

### In the Postman app
1. **Import** (top left) → drag both files in.
2. Top right, choose the environment **Ecommerce Local**.
3. Make sure the system is running (`./scripts/doctor.sh` all green).
4. Run everything at once: hover the collection → **⋯ → Run collection → Run**. Folders run in order 0 → 9.  
   Green ticks = tests passed. Expected: **52 requests, 79 assertions, 0 failed**.
5. Or click requests one by one **in the order of the folders** – they depend on each other:

| Folder | What it shows | Variables it saves for the next requests |
|---|---|---|
| 0. Health checks | gateway up, 9 services in Eureka | – |
| 1. Users and login | register, login, admin login, wrong password | `username`, `token`, `adminToken`, `userId` |
| 2. Products | list, create (admin), update, 403 for normal user | `productId` |
| 3. Favourites | favourite → calls product-service | – |
| 4. Cart | add items, stock limit, 401 without token | – |
| 5. Order + payment + delivery | **checkout** (the big one), stock reduced, cart emptied | `orderId` |
| 6. Delivery lifecycle | admin ships and delivers, order status follows | `shipmentId` |
| 7. Payment failure | declined card → 402, stock restored | `failedOrderId` |
| 8. Security | other user gets 403, `/internal` blocked, fake token | `bobToken` |
| 9. Audit trail | Kafka events stored in MongoDB | – |

**See which services handled a request:** run *"CHECKOUT"* in folder 5, open **Headers** of the response, copy `X-Trace-Id`, then in a terminal:
```bash
./scripts/trace.sh <that-id>
```
(also printed in the Postman Console: *View → Show Postman Console*.)

How the token works: *Login as user* saves the JWT in `token`; the collection sends it automatically as `Authorization: Bearer {{token}}`.
Admin requests use `{{adminToken}}`. Public requests have "No Auth".

### From the terminal (same collection)
```bash
./scripts/postman-test.sh        # needs the Postman CLI (installed here: `postman --version`)
```

### Postman problems
| Message | Reason / fix |
|---|---|
| `Could not get any response` | the system is not running → `./scripts/doctor.sh` |
| `401 Unauthorized` on many requests | token not set / expired (1 h) → run *Login as user* and *Login as ADMIN* again |
| `404` on `/api/products/{{productId}}` or empty `{{orderId}}` | you skipped an earlier request → run the folders in order (or the whole collection) |
| `409` on *Register* | impossible normally (username is random) → the environment has an old `username`; run the request again |
| Environment variable shows `{{token}}` literally | the **Ecommerce Local** environment is not selected (top right) |
| Checks about stock fail (`7`) | another run changed the product; the collection creates its own product each run, so run all folders in order |

---

## 6. Cheat sheet

| What | Where |
|---|---|
| API (use this) | <http://localhost:8080> |
| Eureka (service list) | <http://localhost:8761> |
| Zipkin (traces) | <http://localhost:9411> |
| Admin login | `admin` / `admin123` (how to give others admin rights: [ADMIN_ACCESS.md](ADMIN_ACCESS.md)) |
| MySQL | `jdbc:mysql://localhost:3306`, user `root`, password `123456` (databases `user_db`, `product_db`, `cart_db`, `order_db`, `payment_db`, `shipping_db`, `favourite_db`) |
| MongoDB | `mongodb://localhost:27017/audit_db` |
| Kafka | `localhost:9092`, topic `audit-events` |
| Logs | `logs/<service>.out` (readable) and `logs/<service>.log` (JSON) |
| Docker details (containers, ports, reset) | [docker/README.md](docker/README.md) |
| Prometheus / Grafana / Kibana (optional) | `./scripts/start-infra.sh --observability` → :9090 / :3000 / :5601 |

| Script | Use |
|---|---|
| `scripts/doctor.sh` | check tools, build, containers, services |
| `scripts/start-infra.sh` / `docker compose down` | databases + Kafka + Zipkin on / off |
| `scripts/build.sh` | compile all |
| `scripts/start-services.sh` / `stop-services.sh` | all services on / off |
| `scripts/smoke-test.sh` | 48 automatic API checks (curl) |
| `scripts/postman-test.sh` | the Postman collection from the terminal |
| `scripts/demo-trace.sh` | one purchase + its journey through the services |
| `scripts/trace.sh <id>` | journey of any request by its `X-Trace-Id` |
| `scripts/service.sh crash\|start <name>` | kill / start one service (circuit breaker experiment) |

---

## 7. Run from IntelliJ instead of scripts

1. Open the folder → *Open as Maven project*, JDK 21. Wait for Maven import.  
   (Lombok + MapStruct: IntelliJ → *Settings → Build → Compiler → Annotation Processors → Enable annotation processing*.)
2. `./scripts/start-infra.sh` (databases, Kafka).
3. Run these `*Application` classes in this order: `DiscoveryServerApplication` → `ApiGatewayApplication` → the 8 services.
   Working directory = the module folder (IntelliJ default for Maven modules) so logs go to `../logs`.
4. Test with `requests.http` (IntelliJ) or Postman.

---

## 8. If something is wrong

1. `./scripts/doctor.sh` – it names the failing part and the command that fixes it.
2. Look at the log: `tail -50 logs/<service>.out`
3. **HELP.md → Troubleshooting** has the full table (PKIX, ports in use, Eureka delay, MySQL not running …).
4. Start clean: `./scripts/stop-services.sh && docker compose down -v` and repeat section 3.

Honest status: the local setup (sections 3-5) was run and verified end-to-end on macOS – 48 script checks and the 52-request Postman collection all pass.
The all-in-Docker Compose file, the Kubernetes manifests and Prometheus/Grafana/ELK containers are provided but were not run.
