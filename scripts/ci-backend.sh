#!/usr/bin/env bash
set -euo pipefail

project_dir=$(cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_dir/backend"

./gradlew --no-daemon clean test bootJar

# A missing Docker engine must not silently skip the integration gate.
for suite in MariaDbMigrationTest ReservationConcurrencyTest; do
  report=$(find build/test-results/test -name "TEST-*${suite}.xml" -print -quit)
  if [ -z "$report" ] || grep -q '<skipped' "$report"; then
    echo "ERROR: $suite did not run against Docker." >&2
    exit 1
  fi
done

java -jar build/libs/*-SNAPSHOT.jar > build/ci-startup.log 2>&1 &
app_pid=$!
trap 'kill "$app_pid" 2>/dev/null || true' EXIT

ready=false
for _ in {1..60}; do
  if curl --fail --silent http://127.0.0.1:8080/actuator/health > /dev/null; then
    ready=true
    break
  fi
  if ! kill -0 "$app_pid" 2>/dev/null; then
    break
  fi
  sleep 1
done
if [ "$ready" != true ]; then
  tail -n 100 build/ci-startup.log
  exit 1
fi

# The running application applies Flyway before code generation reads its schema.
JOOQ_CODEGEN=true ./gradlew --no-daemon jooqCodegen compileJava
curl --fail --silent http://127.0.0.1:8080/api/v1/games > /dev/null
