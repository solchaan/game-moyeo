#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
data_file=${GAME_SEED_FILE:-"$script_dir/data/major-games-2026-05.json"}
api_base=${API_BASE_URL:-http://localhost:8080}

: "${ADMIN_USERNAME:?ADMIN_USERNAME is required}"
: "${ADMIN_PASSWORD:?ADMIN_PASSWORD is required}"

for command in curl jq; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $command" >&2
    exit 1
  fi
done

if [ ! -f "$data_file" ]; then
  echo "ERROR: seed data file not found: $data_file" >&2
  exit 1
fi

runtime_dir="$project_dir/backend/build/seed"
mkdir -p "$runtime_dir"
login_file=$(mktemp "$runtime_dir/admin-login.XXXXXX")
response_file=$(mktemp "$runtime_dir/game-response.XXXXXX")
trap 'rm -f "$login_file" "$response_file"' EXIT HUP INT TERM

login_body=$(jq -cn --arg username "$ADMIN_USERNAME" --arg password "$ADMIN_PASSWORD" \
  '{username: $username, password: $password}')
login_status=$(curl --silent --show-error --output "$login_file" --write-out '%{http_code}' \
  --header 'Content-Type: application/json' --data "$login_body" "$api_base/api/v1/auth/admin/login")
if [ "$login_status" != 200 ]; then
  echo "ERROR: administrator login failed with HTTP $login_status" >&2
  jq . "$login_file" >&2 || true
  exit 1
fi
access_token=$(jq -er '.accessToken' "$login_file")

existing_games=$(curl --silent --show-error "$api_base/api/v1/games")
jq -c '.games[]' "$data_file" | while IFS= read -r game; do
  slug=$(printf '%s' "$game" | jq -r '.slug')
  if printf '%s' "$existing_games" | jq -e --arg slug "$slug" '.[] | select(.slug == $slug)' >/dev/null; then
    echo "SKIP: $slug already exists"
    continue
  fi

  status=$(curl --silent --show-error --output "$response_file" --write-out '%{http_code}' \
    --request POST "$api_base/api/v1/admin/games" \
    --header 'Content-Type: application/json' \
    --header "Authorization: Bearer $access_token" \
    --header "Idempotency-Key: major-games-2026-05-$slug" \
    --data "$game")
  if [ "$status" != 201 ]; then
    echo "ERROR: failed to create $slug with HTTP $status" >&2
    jq . "$response_file" >&2 || true
    exit 1
  fi
  option_count=$(jq '.options | length' "$response_file")
  echo "CREATED: $slug ($option_count options)"
done

echo "Major game seed completed (source date: $(jq -r '.asOf' "$data_file"))"
