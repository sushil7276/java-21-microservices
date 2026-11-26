# 09 · Project levels – beginner base and the advanced version

The project is saved in git at two levels. Learn on the **base level** first; when you are comfortable and want less repeated code, look at the **advanced level** and compare.

| Level | Git tag | Commit | What it looks like |
|---|---|---|---|
| **Base (beginner)** – the current state | `v1-beginner-base` | `ddd4598` | Every service is **self-contained**: its own complete `pom.xml` (Spring Boot parent, versions, dependencies), its own complete, fully commented `application.yml`, and its own copies of the small helper classes. **No `common` module, no root `pom.xml`.** |
| **Advanced** | `v2-advanced-shared` | `cec1e20` | A shared **`common`** module (exceptions, circuit-breaker helper, request logging, load-balanced client) and a **root parent `pom.xml`** that holds all versions once. Services have short `pom.xml` files without versions and a small `application.yml` plus a shared `common-defaults.yml`. |

## See the advanced version and the differences

```bash
git diff v2-advanced-shared v1-beginner-base --stat        # which files differ
git diff v2-advanced-shared v1-beginner-base -- user-service/pom.xml
git diff v2-advanced-shared v1-beginner-base -- user-service/src/main/resources/application.yml
```

Look at the advanced version without losing your work (your files change while you look, so commit or stash first):

```bash
git switch --detach v2-advanced-shared      # look around, build, run
git switch main                             # come back to the base level
```

To continue developing from the advanced level, start a branch there: `git switch -c advanced v2-advanced-shared`.

## What changes when you go advanced (and why)

| Topic | Base level | Advanced level |
|---|---|---|
| Versions (Spring Boot, Cloud, MapStruct) | written in every service `pom.xml` (11 places) | once, in the root `pom.xml` |
| Helper classes | copied into each service | once, in `common/` |
| `application.yml` | everything visible in the one file | short file + `common-defaults.yml` imported |
| Building one service | `cd <service> && mvn clean package` | needs the root `pom.xml` and `common` (`mvn -pl <service> -am package`) |
| Fixing a bug in a helper class | change it in every service that has a copy | change it once |

How the advanced parent POM works, step by step: [08-maven-poms.md](08-maven-poms.md#5-later-when-11-copies-of-the-versions-become-a-problem).
