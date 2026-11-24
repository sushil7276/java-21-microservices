#!/usr/bin/env bash
# Shows the JOURNEY of one request through all microservices.
#   ./scripts/trace.sh <traceId>
# Every response has the header  X-Trace-Id  (try:  curl -i http://localhost:8080/api/products | grep -i x-trace-id)
# The same id is written in the log lines of every service that touched the request.
cd "$(dirname "$0")/.."
ID=${1:?usage: ./scripts/trace.sh <traceId>}
echo "Journey of request $ID"
echo "-------------------------------------------------------------------------------------------"
grep -h "$ID" logs/*.out 2>/dev/null | python3 -c '
import sys,re
rows=[]
for line in sys.stdin:
    m=re.match(r"(\S+)\s+\w+\s+\d+ --- \[([^\]]+)\].*? : (.*)",line)
    if m and ("[" in m.group(3) and ("<===" in m.group(3) or "--->" in m.group(3) or "===>" in m.group(3) or "XXX" in m.group(3) or "stored" in m.group(3))):
        rows.append((m.group(1),m.group(3)))
for t,msg in sorted(rows,key=lambda r:r[0]):
    print(t[11:23], msg)
'
echo "-------------------------------------------------------------------------------------------"
echo "Legend:  <===  request arrives in a service      --->  service answers"
echo "         ===>  service calls another service      XXX   call failed (fallback / circuit breaker)"
echo "More detail: open http://localhost:9411 and search for trace id $ID"
