#!/usr/bin/env bash
# Stops every service of this project: the ones started by the scripts (pid files) AND any project service that still
# holds one of the project ports (e.g. one started from IntelliJ, or left over). A program of yours that is NOT part of
# this project and happens to use one of these ports (8080, ...) is never touched; it is only reported.
cd "$(dirname "$0")/.."
ROOT=$(pwd)
source scripts/env.sh
shopt -s nullglob

pids=()
add_pid() { local p=$1 x; for x in ${pids[@]+"${pids[@]}"}; do [[ $x == "$p" ]] && return; done; pids+=("$p"); }

for f in .pids/*.pid; do
  p=$(cat "$f"); kill -0 "$p" 2>/dev/null && is_ours "$p" && add_pid "$p"
done
foreign=()
for s in $SERVICES; do
  port=$(port_of "$s"); p=$(listener_pid "$port")
  [[ -z $p ]] && continue
  if is_ours "$p"; then add_pid "$p"; else foreign+=("port $port is used by another program (pid $p: $(ps -o command= -p "$p" | cut -c1-70)); left alone"); fi
done

if [[ ${#pids[@]} -eq 0 ]]; then echo "nothing of this project is running"; else
  for p in "${pids[@]}"; do kill "$p" 2>/dev/null && echo "stopping pid $p ($(ps -o command= -p "$p" | grep -oE '[a-z-]+-1\.0\.0\.jar|com\.ecommerce\.[A-Za-z.]+' | head -1))"; done
  for _ in $(seq 1 20); do   # graceful shutdown, up to 20 s
    alive=0; for p in "${pids[@]}"; do kill -0 "$p" 2>/dev/null && alive=1; done
    [[ $alive -eq 0 ]] && break; sleep 1
  done
  for p in "${pids[@]}"; do kill -0 "$p" 2>/dev/null && kill -9 "$p" 2>/dev/null && echo "  forced pid $p"; done
fi
rm -f .pids/*.pid
for m in ${foreign[@]+"${foreign[@]}"}; do echo "  note: $m"; done
echo "all stopped"
