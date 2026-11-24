# Novashop – e-commerce microservices

An online shop built from small Spring Boot services behind one API gateway, with a React website and an admin panel.

Java 21 · Spring Boot 4.2.0 · Spring Cloud 2026.0.0 · MySQL · MongoDB · Kafka · Resilience4j · MapStruct ·
React 18 + Redux Toolkit + Tailwind CSS · Zipkin · Prometheus + Grafana

## Architecture

```
 Browser ──► React website :5173 ──┐
 Postman / curl ───────────────────┤
                                   ▼
                          API GATEWAY :8080  (checks the JWT, picks the service)
                                   │                    ┌──────────────────────┐
                                   ├── asks "where is X?" ──►  EUREKA :8761     │ (phone book)
                                   ▼                    └──────────────────────┘
     user · product · cart · order · payment · shipping · favourite · audit
     (each service has its own database; order-service runs a whole purchase)
```

How a request travels, step by step: [ARCHITECTURE.md](ARCHITECTURE.md).

## Services

| Service | Port | Job | Needs |
|---|---|---|---|
| **frontend** (React) | 5173 | website and admin panel | gateway |
| discovery-server | 8761 | Eureka: services register here and find each other | – |
| api-gateway | 8080 | the only entrance: JWT check, routing, circuit breakers | Eureka |
| user-service | 8081 | register, login (creates the JWT), profiles | Eureka, MySQL `user_db` |
| product-service | 8082 | catalogue, stock, product images (links) | Eureka, MySQL `product_db` |
| cart-service | 8083 | shopping cart | Eureka, MySQL `cart_db` |
| order-service | 8084 | runs a purchase: calls user, cart, product, payment, shipping | Eureka, MySQL `order_db`, Kafka |
| payment-service | 8085 | simulated payment | Eureka, MySQL `payment_db` |
| shipping-service | 8086 | delivery and tracking number | Eureka, MySQL `shipping_db` |
| favourite-service | 8087 | wish list | Eureka, MySQL `favourite_db` |
| audit-service | 8088 | stores every event it receives from Kafka | Eureka, MongoDB `audit_db`, Kafka |

Helpers in Docker: MySQL 3306, MongoDB 27017, Kafka 9092, Zipkin 9411 (details: [docker/README.md](docker/README.md)).
Optional: Prometheus 9090 and Grafana 3000.

## Quick start

Requirements: JDK 21, Maven, Docker, Node 18+ (full list: [SETUP.md](SETUP.md)).

```bash
./scripts/start-infra.sh                          # MySQL, MongoDB, Kafka, Zipkin
./scripts/build.sh                                # compile (first time and after code changes)
./scripts/start-services.sh                       # Eureka, gateway and the 8 services
cd frontend && npm install && npm run dev         # website
```

Open <http://localhost:5173>. Everything about running it (one service, restart, stop, monitoring, problems): [START.md](START.md).

## Where things are

| What | Where |
|---|---|
| Website | <http://localhost:5173> |
| API (use this for curl / Postman) | <http://localhost:8080> |
| Service list (all 9 should be UP) | <http://localhost:8761> |
| Traces | <http://localhost:9411> (Zipkin) |
| Dashboards | <http://localhost:3000> (Grafana, optional) |
| Logs | `logs/<service>.out` (readable), `logs/<service>.log` (JSON) |

| Login | Credentials |
|---|---|
| Website admin | `admin` / `admin123` (rights and rules: [ADMIN_ACCESS.md](ADMIN_ACCESS.md)) |
| Website customer | sign up on the site |
| MySQL | `root` / `123456` |
| Grafana | `admin` / `admin` |

## Documentation

| I want to … | Read |
|---|---|
| install the tools and start on a new computer | [SETUP.md](SETUP.md) |
| start, stop, restart, monitor, fix start-up problems | [START.md](START.md) |
| test (smoke test, curl, Postman) | [TESTING.md](TESTING.md) |
| understand request flow, the circuit breaker, the code layout | [ARCHITECTURE.md](ARCHITECTURE.md) |
| manage the admin account and rights | [ADMIN_ACCESS.md](ADMIN_ACCESS.md) |
| reuse the Maven parent-POM setup in another project | [POM_GUIDE.md](POM_GUIDE.md) |
| look inside the Docker containers and databases | [docker/README.md](docker/README.md) |
| import the Postman collection | [postman/README.md](postman/README.md) |
| work on the website code | [frontend/README.md](frontend/README.md) |

## Status

Verified on macOS: the local setup, the website, the admin panel, Prometheus and the Grafana dashboard.
Provided but **not run end-to-end**: the ELK containers, the all-in-Docker compose file (`docker-compose.apps.yml`) and the Kubernetes manifests (`k8s/`).
Demo-grade simplifications: shared JWT secret, simulated payment, `ddl-auto: update`, synchronous order handling with stock compensation only.
