#!/usr/bin/env bash
# Runs the Postman collection from the terminal (same as "Run collection" in the Postman app).
# Needs the Postman CLI:  https://learning.postman.com/docs/postman-cli/postman-cli-installation/
cd "$(dirname "$0")/.."
command -v postman >/dev/null || { echo "Postman CLI not installed - import the files from ./postman into the Postman app instead (see docs/01-setup.md)"; exit 1; }
postman collection run postman/Ecommerce-Microservices.postman_collection.json -e postman/Ecommerce-Local.postman_environment.json
