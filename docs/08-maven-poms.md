# 08 · Maven `pom.xml` – one complete file per service

**Every service folder has its own, complete `pom.xml`. There is no `pom.xml` in the project root and no shared parent project.**
You can open `user-service/` alone in an IDE, copy it to another computer, or give it to another team, and it builds with
`mvn clean package`. The price: the version numbers are written in every service (section 3 shows how to change them).

## 1. Read one pom from top to bottom

Open any `*-service/pom.xml`; the numbered comments in the file follow this order:

| # | Part | What it does |
|---|---|---|
| 1 | `<parent>` = `spring-boot-starter-parent` **4.2.0** | The only version you must choose. Spring Boot then supplies the right version of almost every library, so most `<dependency>` blocks have no `<version>`. |
| 2 | `groupId` / `artifactId` / `version` | The name of this service. The jar is `target/<artifactId>-1.0.0.jar`. |
| 3 | `<properties>` | Java version (21), Spring Cloud version (`2026.0.0`) and, where used, MapStruct (`1.6.3`). |
| 4 | `<dependencyManagement>` | A version list. It adds **nothing** to the program; it only lets `spring-cloud-*` (and MapStruct) be written without a version below. |
| 5 | `<dependencies>` | The libraries this service really uses (web, database, Redis, Kafka, Eureka ...), then the monitoring libraries (Actuator, Prometheus, Zipkin) and, where used, Lombok. |
| 6 | `<build>` | The compiler setup (Lombok + MapStruct, where used) and `spring-boot-maven-plugin`, which makes the runnable jar. |

Which service uses what: look at its `pom.xml`. For example only `user-service`, `product-service` and `cart-service` have Redis, `audit-service` has MongoDB and Kafka,
and `batch-service` has Spring Batch.

## 2. Build and run

```bash
./scripts/build.sh                      # all services, one after the other
./scripts/build.sh user-service         # one service (same as: cd user-service && mvn clean package)
java -jar user-service/target/user-service-1.0.0.jar
```
In IntelliJ: open one service folder (or several, each in its own window) as a Maven project and run its `*Application` class.

## 3. Changing a version

The versions are repeated in the 11 poms, so change them with one command from the project root, then build and test:

```bash
# Spring Boot (the <version> inside <parent>), for example 4.2.0 -> 4.2.1
sed -i '' 's#<version>4.2.0</version>            <!-- THE Spring Boot#<version>4.2.1</version>            <!-- THE Spring Boot#' */pom.xml     # macOS; on Linux use  sed -i  without the ''
# Spring Cloud
sed -i '' 's#<spring-cloud.version>2026.0.0<#<spring-cloud.version>NEW<#' */pom.xml
./scripts/build.sh && ./scripts/restart-service.sh user-service    # then ./scripts/smoke-test.sh
```

**Choose compatible versions** (the step that usually goes wrong):
1. Pick the Spring Boot version (start.spring.io shows the newest).
2. Pick the **Spring Cloud release train built for it**. Open
   `https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dependencies/<train>/spring-cloud-dependencies-<train>.pom` and read `<spring-boot.version>`.
   Here: Cloud `2026.0.0` → Boot `4.2.0`.
3. Do **not** add a version to anything Spring Boot or the Cloud list already manages. Only pin what they do not manage (here: MapStruct).
4. `mvn dependency:tree` shows what a service really uses.

## 4. Common mistakes

| Symptom | Cause / fix |
|---|---|
| Dependency "version is missing" | Spring Boot does not manage it: add `<version>` to that dependency (or a property + `<dependencyManagement>` entry, like MapStruct). |
| `NoSuchMethodError`, odd start-up failures after a version change | Boot and Cloud versions do not belong together (section 3), or the services have **different** versions: change all poms. |
| Maven `PKIX path building failed` | Company proxy: [01-setup.md](01-setup.md#3-company-network-that-re-signs-https-pkix-path-building-failed). |
| Lombok / MapStruct code missing (`cannot find symbol` for getters or `*MapperImpl`) | The `maven-compiler-plugin` block (part 6) was removed or Lombok is not listed before MapStruct. |

## 5. Later: when 11 copies of the versions become a problem

When you have many services you will want the versions in **one** place. The usual solution is a *parent POM*: a root `pom.xml`
(`<packaging>pom</packaging>`) that holds the Spring Boot parent, the properties, the version list and the shared libraries, plus
`<modules>` listing the services. Each service then declares that root as its `<parent>` and needs no versions at all.
You can add it step by step:

1. Create `pom.xml` in the project root with `<packaging>pom</packaging>`, the same `<parent>` (Spring Boot), `<properties>`, `<dependencyManagement>` and the monitoring `<dependencies>` taken from any service pom, and a `<module>` line per service.
2. In each service pom replace the Spring Boot `<parent>` by the root (`com.ecommerce` / `ecommerce-microservices` / `1.0.0`) and delete the blocks that moved (properties, `dependencyManagement`, monitoring dependencies).
3. Build from the root with `mvn clean package` (or `mvn -pl user-service -am package` for one service).
4. A service that is opened alone then needs the root installed once: `mvn -N install`.

This project did exactly that before and was changed back on purpose, to keep every service readable on its own.
The same idea works for shared **code**: a `common` library module, `packaging` jar, listed in the version list and used as a normal dependency.
