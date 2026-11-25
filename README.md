# Novashop – e-commerce microservices

An online shop built from small Spring Boot services behind one API gateway, with a React website and an admin panel.

Java 21 · Spring Boot 4.2.0 · Spring Cloud 2026.0.0 · MySQL · MongoDB · Redis · Spring Batch · Kafka · Resilience4j · MapStruct ·
React 18 + Redux Toolkit + Tailwind CSS · Zipkin · Prometheus + Grafana

```
 Browser ──► React website :5173 ──┐
 Postman / curl ───────────────────┤
                                   ▼
                          API GATEWAY :8080   JWT check · rate limit · routing · circuit breakers
                                   │                    ┌──────────────────────┐
                                   ├── "where is X?" ──►  EUREKA :8761         │ (phone book)
                                   ▼                    └──────────────────────┘
     user · product · cart · order · payment · shipping · favourite · audit · batch
     (each service owns its data; order-service runs a whole purchase)
```

## Quick start

Needs JDK 21, Maven, Docker and Node 18+ ([docs/01-setup.md](docs/01-setup.md) has the full list and the first-time steps).

```bash
./scripts/start-infra.sh                          # MySQL, MongoDB, Kafka, Redis, Zipkin
./scripts/build.sh                                # compile (first time and after code changes)
./scripts/start-services.sh                       # Eureka, gateway and the 9 services
cd frontend && npm install && npm run dev         # website
```

## Services

| Service | Port | Job | Data |
|---|---|---|---|
| **frontend** (React) | 5173 | website and admin panel | – |
| discovery-server | 8761 | Eureka: services register here and find each other | – |
| api-gateway | 8080 | the only entrance: JWT check, rate limit, routing, circuit breakers | Redis |
| user-service | 8081 | register, login (creates the JWT), profiles, login revocation | MySQL `user_db`, Redis |
| product-service | 8082 | catalogue, stock, product images (links), cache | MySQL `product_db`, Redis |
| cart-service | 8083 | shopping cart | Redis |
| order-service | 8084 | runs a purchase: calls user, cart, product, payment, shipping | MySQL `order_db`, Kafka |
| payment-service | 8085 | simulated payment | MySQL `payment_db` |
| shipping-service | 8086 | delivery and tracking number | MySQL `shipping_db` |
| favourite-service | 8087 | wish list | MySQL `favourite_db` |
| audit-service | 8088 | stores every event it receives from Kafka | MongoDB `audit_db`, Kafka |
| batch-service | 8089 | bulk jobs: CSV product import, daily sales report, clean-up | MySQL `batch_db` |

Every service except Eureka also needs Eureka running. Docker helpers and their ports: [docs/07-infrastructure.md](docs/07-infrastructure.md).

## Where things are

| What | Where |
|---|---|
| Website | <http://localhost:5173> |
| API (curl / Postman) | <http://localhost:8080> |
| Service list (all 10 should be UP) | <http://localhost:8761> |
| Traces | <http://localhost:9411> (Zipkin) |
| Dashboards | <http://localhost:3000> (Grafana, optional) |
| Logs | `logs/<service>.out` (readable), `logs/<service>.log` (JSON) |

| Login | Credentials |
|---|---|
| Website admin | `admin` / `admin123` (`admin@ecommerce.local`) – rights and rules: [docs/05-security-and-admin.md](docs/05-security-and-admin.md) |
| Website customer | sign up on the site |
| MySQL | `root` / `123456` |
| Grafana | `admin` / `admin` |

## Documentation

| I want to … | Read |
|---|---|
| install the tools and start on a new computer | [docs/01-setup.md](docs/01-setup.md) |
| start, stop, restart, monitor, configure, fix problems | [docs/02-running.md](docs/02-running.md) |
| test (smoke test, curl, Postman, website) | [docs/03-testing.md](docs/03-testing.md) |
| understand request flow, checkout, circuit breaker, code layout | [docs/04-architecture.md](docs/04-architecture.md) |
| manage the admin account, rights and sessions | [docs/05-security-and-admin.md](docs/05-security-and-admin.md) |
| learn how Redis and Spring Batch are used | [docs/06-redis-and-batch.md](docs/06-redis-and-batch.md) |
| look inside the Docker containers and databases | [docs/07-infrastructure.md](docs/07-infrastructure.md) |
| reuse the Maven parent-POM setup in another project | [docs/08-maven-parent-pom.md](docs/08-maven-parent-pom.md) |
| import the Postman collection | [postman/README.md](postman/README.md) |
| work on the website code | [frontend/README.md](frontend/README.md) |

## Status

Verified on macOS: the local setup, the website, the admin panel, Redis features, the batch jobs, Prometheus and the Grafana dashboard.
Provided but **not run end-to-end**: the ELK containers, the all-in-Docker compose file (`docker-compose.apps.yml`, without batch-service/Redis) and the Kubernetes manifests (`k8s/`).
Demo-grade simplifications: shared JWT secret, simulated payment, `ddl-auto: update`, synchronous order handling with stock compensation only.
