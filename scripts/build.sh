#!/usr/bin/env bash
# Builds services (tests are skipped). Needs Java 21 + Maven.
#   ./scripts/build.sh                        every service
#   ./scripts/build.sh product-service ...    only these services
# There is no root pom.xml: every service folder has its own complete pom.xml, so each one is built on its own
# (the same as:  cd product-service && mvn clean package).
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/env.sh

if [[ $# -eq 0 ]]; then targets=$SERVICES; else
  for s in "$@"; do [[ -n $(port_of "$s") ]] || { echo "unknown service '$s'. Known: $SERVICES"; exit 1; }; done
  targets="$*"
fi

for s in $targets; do
  echo "== building $s"
  mvn -q -DskipTests -f "$s/pom.xml" clean package
done
echo "BUILD SUCCESS ($(echo $targets | wc -w | tr -d ' ') service(s))"
