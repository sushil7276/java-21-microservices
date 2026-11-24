#!/usr/bin/env bash
# Rebuild and restart ONE OR MORE services: the safe way to make a running service use your new code.
# (A running service keeps using the jar it started with, so a rebuild alone changes nothing until it is restarted.)
#   ./scripts/restart-service.sh product-service
#   ./scripts/restart-service.sh user-service product-service
#   ./scripts/restart-service.sh --no-build product-service      just restart, do not rebuild
cd "$(dirname "$0")/.."
ROOT=$(pwd)
source scripts/env.sh

BUILD=1; names=()
for a in "$@"; do if [[ $a == --no-build ]]; then BUILD=0; else names+=("$a"); fi; done
[[ ${#names[@]} -gt 0 ]] || { echo "usage: ./scripts/restart-service.sh [--no-build] <service> [<service> ...]"; echo "services: $SERVICES"; exit 1; }
for n in "${names[@]}"; do [[ -n $(port_of "$n") ]] || { echo "unknown service '$n'. Known: $SERVICES"; exit 1; }; done

# 1. stop (graceful; only if the process on that port is ours)
for n in "${names[@]}"; do
  port=$(port_of "$n"); p=$(listener_pid "$port")
  if [[ -n $p ]]; then
    if ! is_ours "$p"; then echo "port $port is used by another program (pid $p), not touching it. Free it first."; exit 1; fi
    kill "$p" && echo "stopping $n (pid $p)"
    for _ in $(seq 1 20); do kill -0 "$p" 2>/dev/null || break; sleep 1; done
    kill -0 "$p" 2>/dev/null && { kill -9 "$p"; echo "  forced"; }
  fi
done

# 2. build (always refreshes `common` first, see build.sh)
if [[ $BUILD -eq 1 ]]; then ./scripts/build.sh "${names[@]}" || { echo "BUILD FAILED, services are stopped; fix the error and run this again"; exit 1; }; fi

# 3. start
mkdir -p logs .pids
for n in "${names[@]}"; do
  jar="$n/target/$n-1.0.0.jar"
  [[ -f $jar ]] || { echo "missing $jar, run without --no-build"; exit 1; }
  ( cd "$ROOT/$n"; nohup java -jar "target/$n-1.0.0.jar" > "$ROOT/logs/$n.out" 2>&1 & echo $! > "$ROOT/.pids/$n.pid" )
  echo "started $n (pid $(cat .pids/$n.pid))"
done

# 4. wait until each one is UP
rc=0
for n in "${names[@]}"; do
  if wait_health "http://localhost:$(port_of "$n")/actuator/health" 120; then echo "  $n UP"; else echo "  !! $n did not come up, see logs/$n.out"; rc=1; fi
done
[[ $rc -eq 0 ]] && echo "done. The gateway notices a restarted service within ~5-10 s."
exit $rc
