#!/usr/bin/env bash
# Stops the services started by start-services.sh
cd "$(dirname "$0")/.."
shopt -s nullglob
files=(.pids/*.pid)
[[ ${#files[@]} -eq 0 ]] && { echo "nothing running"; exit 0; }
for f in "${files[@]}"; do
  pid=$(cat "$f"); name=$(basename "$f" .pid)
  if kill -0 "$pid" 2>/dev/null; then kill "$pid" && echo "stopping $name ($pid)"; fi
done
# wait up to 20 s for a graceful shutdown, then force
for _ in $(seq 1 20); do
  alive=0; for f in "${files[@]}"; do kill -0 "$(cat "$f")" 2>/dev/null && alive=1; done
  [[ $alive -eq 0 ]] && break; sleep 1
done
for f in "${files[@]}"; do
  pid=$(cat "$f")
  if kill -0 "$pid" 2>/dev/null; then kill -9 "$pid" && echo "  forced $(basename "$f" .pid)"; fi
  rm -f "$f"
done
echo "all stopped"
