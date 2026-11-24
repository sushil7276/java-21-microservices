#!/usr/bin/env bash
# Shared helpers, sourced by the other scripts (not run directly).

# --- 1. Company network that re-signs HTTPS (error "PKIX path building failed") --------------------------------
# If you keep a truststore with your company certificate at ~/.m2/truststore*.jks, Maven is pointed at it
# automatically. Certificate checking stays ON. Nothing happens when no such file exists or MAVEN_OPTS is already set.
if [[ "${MAVEN_OPTS:-}" != *trustStore* ]]; then
  for _ts in "$HOME"/.m2/truststore*.jks; do
    if [[ -f $_ts ]]; then export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=$_ts"; break; fi
  done
fi

# --- 2. The services and their ports -------------------------------------------------------------------------
SERVICES="discovery-server api-gateway user-service product-service cart-service order-service payment-service shipping-service favourite-service audit-service"

port_of() {   # service name -> port (empty for an unknown name)
  case $1 in
    discovery-server) echo 8761;; api-gateway) echo 8080;; user-service) echo 8081;; product-service) echo 8082;;
    cart-service) echo 8083;; order-service) echo 8084;; payment-service) echo 8085;; shipping-service) echo 8086;;
    favourite-service) echo 8087;; audit-service) echo 8088;; *) echo "";;
  esac
}

# port -> pid listening on it (empty when nothing listens; never fails, so it is safe under `set -e`)
listener_pid() { lsof -ti tcp:"$1" -sTCP:LISTEN 2>/dev/null | head -1 || true; }

# true when the pid is a Java process of THIS project (a *-1.0.0.jar or a com.ecommerce.* main class).
# Anything else on a project port (another app of yours) is never touched.
is_ours() {
  local cmd; cmd=$(ps -o command= -p "$1" 2>/dev/null) || return 1
  [[ $cmd =~ -1\.0\.0\.jar || $cmd =~ com\.ecommerce\. ]]
}

# seconds the process has been running (ps prints [[dd-]hh:]mm:ss)
proc_age_seconds() {
  ps -o etime= -p "$1" 2>/dev/null | tr -d ' ' | awk -F'[-:]' '{
    if (NF==4) print $1*86400+$2*3600+$3*60+$4; else if (NF==3) print $1*3600+$2*60+$3; else if (NF==2) print $1*60+$2; else print 0 }'
}
file_age_seconds() {   # seconds since the file was last written
  local m; m=$(stat -f %m "$1" 2>/dev/null || stat -c %Y "$1" 2>/dev/null) || { echo 0; return; }
  echo $(( $(date +%s) - m ))
}
# true when the jar was rebuilt AFTER the running process started, i.e. the service runs OLD code
running_old_jar() {   # name pid
  local jar="$ROOT/$1/target/$1-1.0.0.jar"
  [[ -f $jar ]] || return 1
  [[ $(file_age_seconds "$jar") -lt $(proc_age_seconds "$2") ]]
}

wait_health() {   # url [seconds]
  local n=$(( ${2:-120} / 2 ))
  for _ in $(seq 1 "$n"); do curl -fs -m 2 "$1" >/dev/null 2>&1 && return 0; sleep 2; done
  return 1
}
