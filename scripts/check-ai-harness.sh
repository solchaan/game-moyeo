#!/usr/bin/env sh
set -eu

root_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
harness="$root_dir/AGENTS.md"

if [ ! -f "$harness" ]; then
  echo "ERROR: AGENTS.md AI harness is missing" >&2
  exit 1
fi

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
