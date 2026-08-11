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
