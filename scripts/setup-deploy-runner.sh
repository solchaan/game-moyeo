#!/usr/bin/env bash
set -euo pipefail

export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
export LC_ALL=C
repo=solchaan/game-moyeo
runner_dir=${GAME_MOYEO_RUNNER_DIR:-"$HOME/.local/share/game-moyeo-runner"}
project_dir=$(cd -- "$(dirname -- "$0")/.." && pwd)
env_file=${GAME_MOYEO_ENV_FILE:-"$project_dir/.env.container"}

if [ "$(uname -s)" != Darwin ] || [ "$(uname -m)" != arm64 ]; then
  echo 'This installer targets the current Apple Silicon Mac.' >&2
  exit 1
fi
test -f "$env_file"
docker info > /dev/null
umask 077
mkdir -p "$runner_dir" "$HOME/.config/game-moyeo"
install -m 600 "$env_file" "$HOME/.config/game-moyeo/container.env"

if [ ! -x "$runner_dir/config.sh" ]; then
  archive="$runner_dir/actions-runner-osx-arm64-2.337.0.tar.gz"
  curl --fail --location --retry 3 \
    --output "$archive" \
    https://github.com/actions/runner/releases/download/v2.337.0/actions-runner-osx-arm64-2.337.0.tar.gz
  expected=5a2cd92908a93d7276a194e1de6008099f3e7946f3f8e14aa7a1a7b4a31fdec2
  actual=$(LC_ALL=C shasum -a 256 "$archive" | awk '{print $1}')
  if [ "$actual" != "$expected" ]; then
    echo 'Runner checksum mismatch.' >&2
    exit 1
  fi
  tar -xzf "$archive" -C "$runner_dir"
fi

cd "$runner_dir"
if [ "${1:-}" = --prepare-only ]; then
  echo 'Runner files and private container environment prepared. GitHub registration has not run.'
  exit 0
fi
gh auth status --hostname github.com
if [ ! -f .runner ]; then
  registration_token=$(gh api --method POST "repos/$repo/actions/runners/registration-token" --jq .token)
  ./config.sh --unattended --url "https://github.com/$repo" \
    --token "$registration_token" --name game-moyeo-mac \
    --labels game-moyeo-deploy --work _work
  unset registration_token
fi
if [ ! -f .service ]; then ./svc.sh install; fi
./svc.sh start
./svc.sh status
brew services start colima
echo 'Runner installed. Push the verified workflows and application code to main to deploy.'
