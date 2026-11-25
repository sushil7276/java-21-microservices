# Multi-module Maven + Spring Boot: parent POM inheritance guide

Reusable pattern from this project: **one root POM controls every version; each service POM has no versions.**

## 1. Structure

```
my-project/
├── pom.xml               ← ROOT (parent). packaging = pom. Only place with versions.
├── common/pom.xml        ← shared library (jar) used by services
├── user-service/pom.xml  ← service (child). No versions.
└── order-service/pom.xml
```

Inheritance chain:

```
spring-boot-starter-parent (4.2.0)
        ↓  <parent> in root pom.xml
root pom.xml  (+ imports spring-cloud-dependencies BOM)
        ↓  <parent> in every module
common, user-service, order-service ...
```

## 2. Create the root POM

```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <!-- (A) Spring Boot as parent: gives dependency versions, plugin config, Java defaults -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.2.0</version>           <!-- ← THE Boot version. Change here only. -->
        <relativePath/>                    <!-- always resolve from repository, not ../pom.xml -->
    </parent>

    <groupId>com.mycompany</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>             <!-- REQUIRED: a parent/aggregator is not a jar -->

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2026.0.0</spring-cloud.version>
    </properties>

    <!-- (B) Modules to build, in any order (Maven sorts by dependencies) -->
    <modules>
        <module>common</module>
        <module>user-service</module>
        <module>order-service</module>
    </modules>

    <!-- (C) Version catalogue: declares versions, adds NOTHING to the classpath -->
    <dependencyManagement>
        <dependencies>
            <dependency>                    <!-- Spring Cloud BOM: versions for all spring-cloud-* -->
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>                    <!-- your own shared module -->
                <groupId>com.mycompany</groupId>
                <artifactId>common</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- (D) Real dependencies: EVERY module gets these automatically -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
</project>
```

### Which block does what

| Block | Effect on children |
|---|---|
| `<parent>` (Boot) | Supplies versions for all `spring-boot-starter-*`, plugin versions, UTF-8, `-parameters`, Java level |
| `<dependencyManagement>` | Only **sets the version**. A child must still list the dependency, but without `<version>` |
| `<dependencies>` | **Forced on every child**. Use only for things truly needed everywhere |
| `<pluginManagement>` | Default plugin config; child opts in by listing the plugin |
| `<build><plugins>` | Plugin runs in **every** child |
| `<properties>` | Inherited; children can use `${spring-cloud.version}` |

## 3. Create a child (service) POM

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.mycompany</groupId>
        <artifactId>my-project</artifactId>   <!-- the ROOT, not Spring Boot -->
        <version>1.0.0</version>
    </parent>
    <artifactId>user-service</artifactId>     <!-- no groupId/version: inherited -->

    <dependencies>
        <dependency>
            <groupId>com.mycompany</groupId>
            <artifactId>common</artifactId>                       <!-- no version -->
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>   <!-- no version -->
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId> <!-- no version -->
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>   <!-- makes the runnable fat jar; version comes from Boot parent -->
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

The shared library (`common`) is the same, but its POM has **no** `spring-boot-maven-plugin` (it is a plain jar, not runnable) and uses `<optional>true</optional>` on Spring starters so each service chooses its own.

## 4. Choosing compatible versions (the step that usually goes wrong)

1. Pick the Spring Boot version (start.spring.io shows the latest).
2. Pick the **Spring Cloud release train built for that Boot**. Do not guess from the table on the wiki, which lags:
   open `https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dependencies/<train>/spring-cloud-dependencies-<train>.pom`
   and read the `<spring-boot.version>` property.
   Here: Cloud `2026.0.0` -> Boot `4.2.0`; Cloud `2025.1.x` -> Boot `4.0.x/4.1.x`.
3. Do **not** pin versions of anything the Boot parent or Cloud BOM already manages. Only pin what they don't manage (here: MapStruct).
4. To see what Boot manages: `mvn help:effective-pom` or `mvn dependency:tree`.

## 5. Build and run

```bash
mvn clean install                    # build everything, install `common` into ~/.m2
mvn -pl user-service -am package     # build one module plus what it needs (-am)
mvn -pl common -am install           # refresh `common` + parent POM in ~/.m2 (needed before a service is built or opened on its own)
```
In this project the helper scripts do this for you (`./scripts/build.sh`, `./scripts/restart-service.sh`): see [02-running.md](02-running.md#2-one-service).

## 6. Opening in IDE

* **Open the root folder** (best). IDE sees all modules; `common` resolves automatically.
* Open a single service folder: it must stay inside the repo (the child finds `../pom.xml`) and `common` must be installed in `~/.m2` (section 5).

## 7. Common mistakes

| Symptom | Cause / fix |
|---|---|
| `packaging` error / parent not found | Root forgot `<packaging>pom</packaging>`, or child `<parent>` artifactId/version doesn't match root |
| "Could not find artifact ...:common", or an old `common` is used | `common` not installed or stale in `~/.m2` (section 5) |
| Dependency "version is missing" | It isn't managed by Boot or any BOM; add it to root `<dependencyManagement>` |
| Spring Cloud / Boot `NoSuchMethodError`, odd startup failures | Boot and Cloud train mismatch; see section 4 |
| Child overrides a version and things break | Remove the child `<version>`; let the BOM decide |
| Dependency in root `<dependencies>` unwanted in one module | Move it down into only the modules that need it |
| Parent fetched from internet instead of local | `<relativePath/>` (empty) on Boot parent is correct; on **your own** parent omit it so `../pom.xml` is used |
| Maven "PKIX path building failed" behind a corporate proxy | See [01-setup.md](01-setup.md#3-company-network-that-re-signs-https-pkix-path-building-failed) |

## 8. Upgrading versions (what we did)

Edit only the root POM:

```xml
<version>4.2.0</version>                          <!-- spring-boot-starter-parent -->
<spring-cloud.version>2026.0.0</spring-cloud.version>
```

Then `./scripts/build.sh`, restart all services, run the smoke test ([03-testing.md](03-testing.md)).

## 9. Variations

* **Don't want Boot as parent** (company already has a corporate parent): import the Boot BOM instead:
  ```xml
  <dependencyManagement><dependencies><dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>4.2.0</version><type>pom</type><scope>import</scope>
  </dependency></dependencies></dependencyManagement>
  ```
  You then configure the `spring-boot-maven-plugin` version and the compiler settings yourself.
* **Single service, no modules:** just use the Boot parent plus normal dependencies. Multi-module pays off from about 3 services.
