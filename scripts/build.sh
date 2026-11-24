#!/usr/bin/env bash
# Builds every module (skips tests). Needs Java 21 + Maven.
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -DskipTests clean package
