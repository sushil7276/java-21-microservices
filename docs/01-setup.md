# 01 · Setup – a new computer, or "I forgot everything"

One-time installation and the first start. Daily use: [02-running.md](02-running.md).

## 1. Install once

| Tool | Version | Check | Install (macOS) |
|---|---|---|---|
| Java JDK | **21** | `java -version` | `brew install openjdk@21` |
| Maven | 3.9+ | `mvn -v` | `brew install maven` |
| Node.js | 18+ | `node -v` | `brew install node` |
| Docker Desktop | recent | `docker info` (must not error) | <https://www.docker.com/products/docker-desktop>, then **start it** |
| jq, curl | any | `jq --version` | `brew install jq` |
| Postman | app or CLI | – | <https://www.postman.com/downloads/> (optional) |
| IDE | IntelliJ IDEA | – | open the folder as a Maven project (optional) |

- **Linux:** `sudo apt install openjdk-21-jdk maven nodejs jq curl` plus Docker Engine.
- **Windows:** the helper scripts are bash. Use WSL2 (Ubuntu) with Docker Desktop's WSL integration, or Git Bash.
  Without bash you can still run the apps from IntelliJ (section 5). Only macOS was tested.

## 2. Get the code

```bash
git clone <your-repository-url> Microservices
cd Microservices
```

## 3. Company network that re-signs HTTPS (`PKIX path building failed`)

Maven (and Docker) stop with this error when a company proxy replaces HTTPS certificates and Java does not trust the company certificate.

- **Scripts:** `build.sh` and `restart-service.sh` automatically use a truststore at `~/.m2/truststore*.jks` when one exists.
- **Other Maven commands:** `export MAVEN_OPTS="-Djavax.net.ssl.trustStore=$HOME/.m2/truststore-netskope.jks"`
  (adjust the file name), or on macOS `export MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=KeychainStore"`.
- **Linux / Windows:** import the company root certificate into the JDK `cacerts`.
- Never switch certificate checking off.
- **npm** can hang when its registry is the company one: `npm install --registry https://registry.npmjs.org`.

## 4. First start, step by step

Ports must be free first (list: [README](../README.md#services) and [07-infrastructure.md](07-infrastructure.md#what-runs-in-docker); a port clash is fixed in [02-running.md](02-running.md#6-troubleshooting)).

| # | Command | What happens | You should see |
|---|---|---|---|
| 1 | `./scripts/doctor.sh` | checks tools (only looks, changes nothing) | no red `MISSING` in section 1 |
| 2 | `./scripts/start-infra.sh` | pulls images (2–5 min the first time), starts the containers | all containers `healthy` |
| 3 | `./scripts/build.sh` | downloads libraries, compiles 12 modules (3–5 min the first time) | `BUILD SUCCESS` |
| 4 | `./scripts/start-services.sh` | starts Eureka, then everything else | `:8080 UP` … `:8089 UP`, then `gateway routes to the services ... OK` |
| 5 | `./scripts/doctor.sh` | everything green | sections 3 and 4 all `OK` |
| 6 | `cd frontend && npm install && npm run dev` | starts the website | <http://localhost:5173> |

Check <http://localhost:8761>: all services **UP**. Then run the tests: [03-testing.md](03-testing.md).

Created automatically on first start: the databases and tables, the admin user, and ten demo products with photos and prices in
rupees (only when the product table is empty).

Already have your own MySQL or MongoDB on 3306 / 27017? Use it instead of the container: [07-infrastructure.md](07-infrastructure.md#use-your-own-mysql-or-mongodb).

## 5. Run from IntelliJ instead of the scripts

1. Open the folder as a Maven project with JDK 21 and wait for the import.
   Enable *Settings → Build → Compiler → Annotation Processors → Enable annotation processing* (Lombok and MapStruct).
2. `./scripts/start-infra.sh` (databases, Kafka, Redis).
3. Run the `*Application` classes in this order: `DiscoveryServerApplication` → `ApiGatewayApplication` → the 9 services.
   Keep the working directory at the module folder (IntelliJ's default) so logs go to `../logs`.
4. Test with `requests.http` (IntelliJ) or Postman.

## 6. Something is wrong?

Run `./scripts/doctor.sh`: it names the failing part and the command that fixes it. Everything else is in the
troubleshooting table of [02-running.md](02-running.md#6-troubleshooting).
