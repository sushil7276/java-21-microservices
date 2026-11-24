#!/usr/bin/env bash
# Builds modules (tests are skipped). Needs Java 21 + Maven.
#   ./scripts/build.sh                        everything
#   ./scripts/build.sh product-service ...    only these services
# The shared `common` library and the parent POM are ALWAYS rebuilt and installed into ~/.m2 first, so a service that is
# built (or opened in an IDE) on its own can never pick up an old copy of them.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/env.sh

mvn -q -DskipTests -pl common -am install
if [[ $# -eq 0 ]]; then
  mvn -DskipTests clean package
else
  for s in "$@"; do [[ -n $(port_of "$s") ]] || { echo "unknown service '$s'. Known: $SERVICES"; exit 1; }; done
  mvn -DskipTests -pl "$(IFS=,; echo "$*")" clean package
fi
