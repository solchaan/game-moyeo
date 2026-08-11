#!/usr/bin/env sh
set -eu

root_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
harness="$root_dir/AGENTS.md"
backend_dir="$root_dir/backend"

if [ ! -f "$harness" ]; then
  echo "ERROR: AGENTS.md AI harness is missing" >&2
  exit 1
fi

for backend_path in \
  "$backend_dir/build.gradle" \
  "$backend_dir/settings.gradle" \
  "$backend_dir/gradlew" \
  "$backend_dir/src/main/resources/db/migration"; do
  if [ ! -e "$backend_path" ]; then
    echo "ERROR: backend project path is missing: $backend_path" >&2
    exit 1
  fi
done

required_terms='Clean Architecture
MariaDB
JPA
Flyway
jOOQ
REST API
Problem Details
Idempotency-Key
reserved_count
대용량 트래픽'

missing=0
old_ifs=$IFS
IFS='
'
for term in $required_terms; do
  if ! grep -Fq "$term" "$harness"; then
    echo "ERROR: AI harness is missing required policy: $term" >&2
    missing=1
  fi
done
IFS=$old_ifs

if [ "$missing" -ne 0 ]; then
  exit 1
fi

echo "AI harness policy check passed"

"$root_dir/scripts/check-frontend-harness.sh"
