#!/usr/bin/env sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)

use_docker=false
install_missing=false
env_file="$project_dir/.env"

usage() {
  echo "Usage: ./scripts/run-local.sh [--docker] [--install] [--env FILE]"
  echo
  echo "  --docker       Start MariaDB, Redis and Kafka with Docker Compose"
  echo "  --install      Install missing Redis with Homebrew on macOS"
  echo "  --env FILE     Load environment variables from FILE (default: .env)"
  echo "  -h, --help     Show this help"
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --docker)
      use_docker=true
      shift
      ;;
    --install)
      install_missing=true
      shift
      ;;
    --env)
      if [ "$#" -lt 2 ]; then
        echo "ERROR: --env requires a file path" >&2
        exit 2
      fi
      env_file=$2
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "ERROR: unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [ -f "$env_file" ]; then
  echo "Loading environment from $env_file"
  set -a
  # The env file is local and trusted. Never commit it.
  # shellcheck disable=SC1090
  . "$env_file"
  set +a
fi

: "${DB_URL:=jdbc:mariadb://localhost:3306/gamemoyeo}"
: "${DB_USERNAME:=gamemoyeo}"
: "${DB_PASSWORD:=local_password}"
: "${REDIS_HOST:=localhost}"
: "${REDIS_PORT:=6379}"
: "${SECURITY_ENABLED:=false}"
: "${SPRING_PROFILES_ACTIVE:=local}"

export DB_URL DB_USERNAME DB_PASSWORD REDIS_HOST REDIS_PORT SECURITY_ENABLED

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $1" >&2
    exit 1
  fi
}

wait_for_port() {
  host=$1
  port=$2
  service_name=$3
  attempts=30

  while [ "$attempts" -gt 0 ]; do
    if nc -z "$host" "$port" >/dev/null 2>&1; then
      echo "$service_name is ready at $host:$port"
      return 0
    fi
    attempts=$((attempts - 1))
    sleep 1
  done

  echo "ERROR: $service_name did not become ready at $host:$port" >&2
  return 1
}

wait_for_port_quietly() {
  host=$1
  port=$2
  attempts=${3:-10}

  while [ "$attempts" -gt 0 ]; do
    if nc -z "$host" "$port" >/dev/null 2>&1; then
      return 0
    fi
    attempts=$((attempts - 1))
    sleep 1
  done
  return 1
}

start_standalone_redis() {
  require_command redis-server
  redis_runtime_dir="$project_dir/build/local-redis"
  mkdir -p "$redis_runtime_dir"

  echo "Homebrew Redis service failed; starting a project-local Redis without optional modules..."
  brew services stop redis >/dev/null 2>&1 || true
  redis-server \
    --bind 127.0.0.1 \
    --protected-mode yes \
    --port "$REDIS_PORT" \
    --daemonize yes \
    --appendonly yes \
    --dir "$redis_runtime_dir" \
    --pidfile "$redis_runtime_dir/redis.pid" \
    --logfile "$redis_runtime_dir/redis.log"
  wait_for_port "$REDIS_HOST" "$REDIS_PORT" Redis
}

ensure_java() {
  require_command java
  java_major=$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}')
  if [ -z "$java_major" ] || [ "$java_major" -lt 21 ]; then
    echo "ERROR: Java 21 or newer is required" >&2
    exit 1
  fi
  echo "Java $java_major detected"
}

start_with_docker() {
  require_command docker
  echo "Starting MariaDB, Redis and Kafka with Docker Compose..."
  docker compose -f "$project_dir/compose.yml" up -d
  wait_for_port localhost 3306 MariaDB
  wait_for_port localhost 6379 Redis
}

start_homebrew_service() {
  formula=$1
  port=$2
  service_name=$3

  if nc -z localhost "$port" >/dev/null 2>&1; then
    echo "$service_name is already running"
    return
  fi

  require_command brew
  if ! brew list --versions "$formula" >/dev/null 2>&1; then
    if [ "$install_missing" != true ]; then
      echo "ERROR: $service_name is not installed." >&2
      echo "Run again with --install or execute: brew install $formula" >&2
      exit 1
    fi
    echo "Installing $service_name with Homebrew..."
    brew install "$formula"
  fi

  echo "Starting $service_name with Homebrew..."
  brew services start "$formula"
  if wait_for_port_quietly localhost "$port" 10; then
    echo "$service_name is ready at localhost:$port"
  elif [ "$formula" = redis ]; then
    start_standalone_redis
  else
    echo "ERROR: $service_name did not become ready at localhost:$port" >&2
    exit 1
  fi
}

verify_database() {
  require_command mariadb

  db_host=$(printf '%s' "$DB_URL" | sed -E 's#^jdbc:mariadb://([^:/?]+).*#\1#')
  db_port=$(printf '%s' "$DB_URL" | sed -nE 's#^jdbc:mariadb://[^:/?]+:([0-9]+).*#\1#p')
  db_name=$(printf '%s' "$DB_URL" | sed -E 's#^jdbc:mariadb://[^/]+/([^?]+).*#\1#')
  if [ -z "$db_port" ]; then
    db_port=3306
  fi

  wait_for_port "$db_host" "$db_port" MariaDB

  defaults_file=$(mktemp "${TMPDIR:-/tmp}/gamemoyeo-mariadb.XXXXXX")
  chmod 600 "$defaults_file"
  trap 'rm -f "$defaults_file"' EXIT HUP INT TERM
  {
    echo "[client]"
    echo "user=$DB_USERNAME"
    echo "password=$DB_PASSWORD"
    echo "host=$db_host"
    echo "port=$db_port"
  } > "$defaults_file"

  if ! mariadb --defaults-extra-file="$defaults_file" -N -e \
    "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '$db_name'" \
    | grep -Fx "$db_name" >/dev/null 2>&1; then
    echo "ERROR: database '$db_name' is missing or credentials are invalid." >&2
    echo "Create it first or update DB_URL, DB_USERNAME and DB_PASSWORD in $env_file" >&2
    exit 1
  fi
  rm -f "$defaults_file"
  trap - EXIT HUP INT TERM
  echo "Database '$db_name' is accessible"
}

verify_redis() {
  wait_for_port "$REDIS_HOST" "$REDIS_PORT" Redis
}

ensure_java
require_command nc

if [ "$use_docker" = true ]; then
  start_with_docker
else
  start_homebrew_service mariadb 3306 MariaDB
  start_homebrew_service redis 6379 Redis
fi

verify_database
verify_redis

echo "Starting gameMoyeo with Spring profile '$SPRING_PROFILES_ACTIVE'..."
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo "Health:     http://localhost:8080/actuator/health"
cd "$project_dir"
exec ./gradlew bootRun --args="--spring.profiles.active=$SPRING_PROFILES_ACTIVE"
