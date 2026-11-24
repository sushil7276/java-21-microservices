# E-commerce Microservices

Spring Boot 4 / Java 21 implementation of the *Ecommerce Microservices Architecture* diagram:
API Gateway + JWT security, Eureka, 7 business services (MySQL), Kafka → Audit service (MongoDB),
Resilience4j circuit breakers, MapStruct mappers, Zipkin, Prometheus/Grafana, ELK, Docker and Kubernetes.

```bash
./scripts/start-infra.sh      # MySQL, MongoDB, Kafka, Zipkin in Docker
./scripts/build.sh            # build all modules
./scripts/start-services.sh   # Eureka, gateway and the 8 services
./scripts/smoke-test.sh       # end-to-end test through the gateway
./scripts/demo-trace.sh       # make one purchase and see which service handled it
./scripts/doctor.sh           # what is installed / running?
```
* **[START.md](START.md)** – how to start everything, one service, or the frontend (and common start-up problems)
* **[SETUP.md](SETUP.md)** – new computer / forgot everything / Postman testing
* **[HELP.md](HELP.md)** – start, test, circuit breaker experiment, how to read the code
* **[FLOW.md](FLOW.md)** – which request goes to which service
