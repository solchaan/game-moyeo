# game-moyeo

## Repository structure

```text
gameMoyeo/
├── backend/             # Spring Boot, Gradle, Flyway, JPA, jOOQ
├── frontend/            # React, TypeScript, Vite
├── scripts/             # 루트 실행 및 하네스 검사
├── docs/                # 아키텍처 문서
├── compose.yml          # 로컬 MariaDB, Redis, Kafka
└── AGENTS.md            # 저장소 공통 AI 개발 규칙
```

## Local run

MariaDB가 이미 준비되어 있다면 Redis를 포함한 필요한 Homebrew 서비스를 확인하고 애플리케이션을 실행합니다.

```bash
./scripts/run-local.sh --install
```

이후 실행부터는 설치 옵션 없이 사용할 수 있습니다.

```bash
./scripts/run-local.sh
```

Homebrew Redis 설정에 설치되지 않은 optional module이 남아 서비스 시작이 실패하면, 스크립트는 해당 서비스를 중지하고 `backend/build/local-redis`에서 프로젝트 전용 Redis를 모듈 없이 실행합니다.

Docker가 설치된 환경에서는 MariaDB, Redis, Kafka를 Compose로 실행할 수 있습니다.

```bash
./scripts/run-local.sh --docker
```

환경변수를 변경하려면 `.env.example`을 `.env`로 복사한 뒤 수정합니다. `.env`는 Git에서 제외됩니다.

```bash
cp .env.example .env
./scripts/run-local.sh
```

백엔드만 직접 실행하거나 검사할 때는 `backend` 디렉터리의 Gradle Wrapper를 사용합니다.

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew clean check
```

## Frontend

React 프런트엔드는 `frontend` 디렉터리에 있습니다. 백엔드를 8080 포트에서 실행한 뒤 별도 터미널에서 시작합니다.

```bash
cd frontend
npm install
npm run dev
```

개발 서버는 `http://localhost:5173`에서 실행되며 `/api`와 `/oauth2` 요청을 백엔드로 프록시합니다. 다른 API 주소를 사용하는 배포 환경에서는 `VITE_API_BASE_URL`을 설정합니다.

```bash
cp frontend/.env.example frontend/.env
```

현재 UI는 게임 목록·상세, 모임 목록·상세·생성·수정·삭제, 소셜 로그인 콜백을 지원합니다. 참여 신청/예약은 백엔드 예약 API가 추가된 뒤 연결할 수 있도록 상세 화면에서 비활성 상태로 표시합니다.

## Caddy + Docker 실행

`compose.app.yml`은 Caddy(프론트엔드 정적 파일 + API 프록시), Java 21 백엔드,
MariaDB, Redis를 함께 실행합니다. 기존 `compose.yml`은 로컬 개발 인프라용입니다.
현재 애플리케이션에는 Kafka를 사용하는 코드가 없어 앱 구성에서는 제외합니다.

macOS에서는 Colima를 Docker 엔진으로 사용합니다.

```bash
brew install docker docker-compose docker-buildx colima caddy
colima start --cpu 4 --memory 6 --disk 40 --vm-type vz --mount-type virtiofs
```

Docker가 Compose와 Buildx를 찾지 못하면 `~/.docker/config.json`의
`cliPluginsExtraDirs`에 `/opt/homebrew/lib/docker/cli-plugins`를 추가합니다.

Git에서 제외되는 `.env.container`에 다음 값을 설정합니다. 비밀번호와 JWT secret은
각각 `openssl rand -hex 32`로 생성한 별도의 값을 사용합니다.

```dotenv
APP_HTTP_PORT=5173
APP_DB_PASSWORD=<random-password>
APP_DB_ROOT_PASSWORD=<different-random-password>
APP_JWT_SECRET=<random-secret>
APP_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
APP_LOGIN_REDIRECT_URI=http://localhost:5173/oauth/callback
```

```bash
docker compose --env-file .env.container -f compose.app.yml up -d --build --wait
docker compose --env-file .env.container -f compose.app.yml ps
docker compose --env-file .env.container -f compose.app.yml logs --tail=100 backend caddy
```

접속 주소는 `http://localhost:5173`이며 `/api/*` 요청은 내부 백엔드로 전달됩니다.
React 경로를 직접 열거나 새로고침해도 Caddy가 `index.html`을 제공합니다.
로컬 Vite가 5173을 사용하고 있으면 해당 프로세스를 종료하거나 `APP_HTTP_PORT`를 변경합니다.
포트를 바꿀 때는 `APP_ALLOWED_ORIGINS`, `APP_LOGIN_REDIRECT_URI`도 맞춰 변경합니다.

외부 공유는 `cloudflared tunnel --url http://127.0.0.1:5173`으로 연결합니다.
터널 HTTPS 주소를 `APP_ALLOWED_ORIGINS`에 추가하고 `APP_LOGIN_REDIRECT_URI`를
`https://발급된-주소/oauth/callback`으로 설정한 뒤 위 `up` 명령을 다시 실행합니다.
TLS는 Cloudflare 터널에서 처리하며 Caddy는 로컬 HTTP를 제공합니다.

코드를 수정하면 `up -d --build --wait`로 다시 빌드합니다. 개발 서버의 자동 반영은 사용하지 않습니다.
DB와 Redis는 이름 있는 볼륨에 보관되며 호스트 포트를 공개하지 않습니다.
중지는 `docker compose --env-file .env.container -f compose.app.yml stop`,
재시작은 `start`를 사용합니다. `down -v`는 저장된 데이터를 삭제하므로 사용하지 마세요.
macOS 재부팅 후에는 `colima start`를 실행해야 합니다.

기존 호스트 DB 데이터는 자동으로 가져오지 않습니다. 이전 시에는 기존 앱의 쓰기를 중지하고
`mariadb-dump --single-transaction`으로 별도 백업을 만든 뒤 비어 있는 컨테이너 DB에 복원합니다.
로컬 이전 백업 위치는 Git과 이미지 빌드에서 제외되는 `.runtime/`입니다.
원본 호스트 DB는 그대로 남으며, 이전 후 새 데이터는 컨테이너 DB에 저장됩니다.
JWT secret이 변경되면 기존 사용자는 다시 로그인해야 합니다.

구성 참고: [Colima 설치](https://colima.run/docs/installation/),
[Caddy SPA 및 API 프록시 패턴](https://caddyserver.com/docs/caddyfile/patterns).

## GitHub Actions 자동 배포

`.github/workflows/pipeline.yml`은 `main`의 push, pull request, 수동 실행에서
백엔드와 프론트엔드 검사를 병렬로 실행합니다. 각각의 상세 구성은 `backend.yml`,
`frontend.yml`에 있습니다.

- 백엔드: Java 21 테스트, Docker 기반 마이그레이션·60명 동시 예약 테스트,
  앱 기동과 Flyway 적용, jOOQ 생성·컴파일, 백엔드 Docker 이미지 빌드.
- 프론트엔드: npm 설치, TypeScript 검사·빌드, Caddy 이미지 빌드·설정 검증,
  데스크톱·태블릿·모바일 Playwright 브라우저 검사.
- 배포: 두 검사 모두 성공한 `solchaan/game-moyeo`의 최신 `main` 커밋만
  `game-moyeo-deploy` 라벨의 Apple Silicon Mac runner에서 배포합니다.
  PR에서는 GitHub 제공 Ubuntu runner로 검사만 실행합니다.

현재 Mac의 runner 등록은 다음 명령으로 수행합니다. GitHub 로그인에는 해당 저장소의
runner를 관리할 권한이 필요합니다. 만료된 인증은 먼저 갱신합니다.

```bash
gh auth login -h github.com
bash scripts/setup-deploy-runner.sh
```

설치 스크립트는 공식 runner의 체크섬을 확인하고 로그인 시 시작되는 서비스로 등록합니다.
runner 위치는 `~/.local/share/game-moyeo-runner`, 비밀 환경 파일은
`~/.config/game-moyeo/container.env`입니다. 이 파일은 runner checkout 밖에 보관합니다.
설정 변경 시 이 파일도 갱신하거나 `GAME_MOYEO_ENV_FILE`로 다른 파일을 지정하세요.
인증 전 파일 준비만 하려면 `--prepare-only` 옵션을 사용합니다.

워크플로와 앱 변경사항을 GitHub `main`에 push하면 검사 후 자동 배포가 시작됩니다.
Mac이 켜져 있고 Colima 및 runner 서비스가 실행 중이어야 합니다.
자동 배포에서는 개발 작업 폴더를 덮어쓰지 않고 runner의 별도 checkout을 사용합니다.
현재 trycloudflare 프로세스가 실행 중이면 같은 5173 포트로 새 컨테이너에 연결됩니다.
임시 터널의 재부팅 후 자동 복구나 영구 도메인 발급은 이 파이프라인에 포함되지 않습니다.

배포 스크립트 `scripts/deploy-containers.sh`는 앱이 실행되는 동안 새 이미지를 빌드하고,
DB를 `~/.local/state/game-moyeo/backups/`에 백업한 뒤 컨테이너를 교체합니다.
health check 또는 API 확인에 실패하면 이전 앱 이미지로 복구합니다.
DB 볼륨은 유지하며 DB 마이그레이션은 자동으로 되돌리지 않습니다. 새 마이그레이션은
이전 앱과 호환되도록 작성해야 합니다. 단일 인스턴스 교체 중에는 짧은 접속 중단이 발생합니다.
백업에는 사용자 데이터가 포함되므로 로컬 권한을 제한하고 별도의 보관 주기를 적용하세요.

프론트엔드 브라우저 검사는 `cd frontend && npm run test:e2e`로 실행합니다.
처음에는 `npx playwright install chromium`이 필요합니다. 실행 중인 Caddy에 대해 검사하려면
`PLAYWRIGHT_BASE_URL=http://127.0.0.1:5173 npm run test:e2e`를 사용합니다.
브라우저 테스트의 API 응답은 테스트 안에서만 대체하며 실제 DB를 수정하지 않습니다.

## Major game seed

2026년 5월 31일 기준 한국 주요 온라인 게임과 모드·역할·티어 등의 초기 카탈로그는 관리자 API로 등록할 수 있습니다. 동일한 slug가 이미 있으면 건너뜁니다.

```bash
ADMIN_USERNAME=admin \
ADMIN_PASSWORD='관리자 비밀번호' \
./scripts/seed-major-games.sh
```

다른 API 주소를 사용할 때는 `API_BASE_URL`을 함께 지정합니다. 기준 데이터는 `scripts/data/major-games-2026-05.json`에 있습니다.

## AI harness

- 백엔드와 저장소 공통 규칙: `AGENTS.md`
- Spring Boot 프로젝트: `backend/`
- React 프론트엔드 규칙: `frontend/AGENTS.md`
- 전체 하네스 검사: `./scripts/check-ai-harness.sh`
