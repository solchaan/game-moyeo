#!/usr/bin/env bash
set -euo pipefail

export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
project_dir=$(cd -- "$(dirname -- "$0")/.." && pwd)
env_file=${GAME_MOYEO_ENV_FILE:-"$HOME/.config/game-moyeo/container.env"}
backup_dir=${GAME_MOYEO_BACKUP_DIR:-"$HOME/.local/state/game-moyeo/backups"}
if [ ! -f "$env_file" ]; then
  echo "ERROR: Set GAME_MOYEO_ENV_FILE to the existing container environment file." >&2
  exit 1
fi
cd "$project_dir"
compose=(docker compose --env-file "$env_file" -f compose.app.yml)
"${compose[@]}" config --quiet
docker info > /dev/null

backend_container=$("${compose[@]}" ps -q backend)
caddy_container=$("${compose[@]}" ps -q caddy)
old_backend=''
old_caddy=''
if [ -n "$backend_container" ]; then old_backend=$(docker inspect --format '{{.Image}}' "$backend_container"); fi
if [ -n "$caddy_container" ]; then old_caddy=$(docker inspect --format '{{.Image}}' "$caddy_container"); fi

# Build completely while the current app continues serving requests.
"${compose[@]}" build backend caddy

umask 077
mkdir -p "$backup_dir"
backup_file=$(mktemp "$backup_dir/pre-deploy-$(date -u +%Y%m%dT%H%M%SZ).XXXXXX")
# These variables expand inside the database container, not on the runner.
# shellcheck disable=SC2016
"${compose[@]}" exec -T mariadb sh -c \
  'MYSQL_PWD="$MARIADB_PASSWORD" exec mariadb-dump -u "$MARIADB_USER" --single-transaction --routines --triggers --skip-add-drop-table gamemoyeo' \
  > "$backup_file"
test -s "$backup_file"
echo "Database backup: $backup_file"

rollback() {
  echo 'Deployment failed; restoring the previous application images.' >&2
  "${compose[@]}" logs --tail=80 backend caddy >&2 || true
  if [ -n "$old_backend" ] && [ -n "$old_caddy" ]; then
    docker image tag "$old_backend" game-moyeo-app-backend:latest
    docker image tag "$old_caddy" game-moyeo-app-caddy:latest
    "${compose[@]}" up -d --no-build --pull never --wait --wait-timeout 180 backend caddy || true
  fi
  echo 'Database volumes were preserved. Schema migrations are not automatically reversed.' >&2
  exit 1
}

if ! "${compose[@]}" up -d --no-build --wait --wait-timeout 180; then
  rollback
fi
if ! "${compose[@]}" exec -T caddy wget -q -O /dev/null http://127.0.0.1/api/v1/games; then
  rollback
fi
"${compose[@]}" ps
echo 'Deployment healthy: Caddy, backend, MariaDB and Redis.'
