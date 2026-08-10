#!/usr/bin/env sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
harness="$project_dir/frontend/AGENTS.md"

if [ ! -f "$harness" ]; then
  echo "ERROR: frontend/AGENTS.md is missing" >&2
  exit 1
fi

required_terms='React
TypeScript strict mode
TanStack Query
Problem Details
Idempotency-Key
WCAG 2.2 AA
Core Web Vitals
서버 중심 계산 원칙
프론트에서 계산하지 않는 값
access token
cursor
Playwright'

missing=0
old_ifs=$IFS
IFS='
'
for term in $required_terms; do
  if ! grep -Fq "$term" "$harness"; then
    echo "ERROR: frontend harness is missing required policy: $term" >&2
    missing=1
  fi
done
IFS=$old_ifs

if [ "$missing" -ne 0 ]; then
  exit 1
fi

echo "Frontend harness policy check passed"
