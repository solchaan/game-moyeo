# 순차 고도화: 내 모임부터 커뮤니티 관리까지

## 사용자 흐름

1. `/my-meetups`: 참여 예정(시작순), 내가 만든 모임, 지난 모임. 상세 화면에서 취소하며, 예정 모임은 ICS 다운로드와 준비 완료를 제공한다. 시작 시간이 지난 기록이 실제 게임 종료나 출석을 보증하지 않는다.
2. `/notifications`: 참가/취소, 모임 변경/삭제, 시작 30분 전 안내, 대기 우선권 안내. 사이트 안의 알림함이며 이메일·모바일 푸시·Discord 발송은 포함하지 않는다. 화면을 여는 동안 30초 주기로 갱신한다.
3. `/recommendations`: 저장된 주 포지션 공석 100점, 부 포지션 공석 60점, 필터 일치 0점 순. 동점은 시작 시각/ID 순. 게임·서버·모드·선택한 티어·성향·시작 시간 범위를 서버에서 적용한다. 포지션 프로필은 현재 롤만 지원하므로 다른 게임은 조건과 시작순으로 제공한다. 티어 필터는 사용자가 선택한 값이며 외부 계정 검증값이 아니다.
4. 모임 상세의 `빈자리 대기 신청·현황`: 만석 모임에 신청. 취소로 빈자리가 나면 대기 순서대로 한 명에게 최대 5분의 우선권을 부여한다. 모집 마감 또는 시작 10분 전이면 그보다 짧아진다. 자동 참가 대신 기존 예약 API에서 확정한다. 직접 참가도 우선권을 통과해야 한다. 만료·취소 시 다음 사람에게 넘어가며, 대기자는 정원에 포함하지 않는다.
5. 내 모임에서 `모집 템플릿 저장` → `/templates`: 저장 당시 조건을 재사용한다. 단일 모임 또는 최대 8회 주간 모임을 한 트랜잭션에서 생성한다. 서버가 IANA 시간대와 `plusWeeks`로 계산한다. 무기한 자동 발행이 아니라 유한한 일정을 미리 생성하는 방식이다. 참가자는 복사하지 않으며 각 모임은 독립 수정/취소한다. 예전 모집 마감은 복사하지 않는다.
6. `/companions`: 과거 참가 확정 기록 기반 재참여 의향, 차단, 신고. 차단 목록에서 해제 가능. 추천과 새 참가에서 양방향 차단을 확인하며 기존 참가자를 자동 퇴장시키지 않는다. 신고는 관리자 `/admin/reports`에서 사유와 처리자를 기록해 처리/기각한다. 자동 패널티는 없다.

## 새 API

- `GET /api/v1/members/me/meetups?filter=UPCOMING|HOSTED|PAST&cursor=&size=`
- `PUT /api/v1/meetups/{id}/readiness` — `{ready: boolean}`
- `GET /api/v1/members/me/notifications?cursor=&size=`
- `PUT /api/v1/members/me/notifications/{id}/read`
- `GET /api/v1/members/me/recommendations?gameId=&regionId=&modeId=&tierId=&playStyle=&from=&to=&cursor=&size=`
- `GET|PUT|DELETE /api/v1/meetups/{id}/waitlist/me`
- `PUT /api/v1/members/me/templates/from/{meetupId}`
- `GET /api/v1/members/me/templates?cursor=&size=`
- `DELETE /api/v1/members/me/templates/{id}`
- `POST /api/v1/members/me/templates/{id}/meetups` — `{startsAt, weeks: 1..8, timeZone}`, `Idempotency-Key` 필수
- `GET /api/v1/members/me/companions?cursor=&size=`
- `GET /api/v1/members/me/blocks?cursor=&size=`
- `PUT /api/v1/members/me/blocks/{memberId}` — `{enabled: boolean}`
- `PUT /api/v1/members/me/meetups/{meetupId}/endorsements/{memberId}` — `{enabled: boolean}`
- `PUT /api/v1/members/me/meetups/{meetupId}/reports/{memberId}` — `{reason: ABUSE|SPAM|NO_SHOW|OTHER, details}`
- `GET /api/v1/admin/reports?cursor=&size=`
- `PUT /api/v1/admin/reports/{id}/resolution` — `{status: RESOLVED|DISMISSED, resolution}`

모든 개인 API는 인증 주체 ID를 사용한다. 관리자 API는 ADMIN 권한이 필요하다. 목록은 `items/nextCursor/hasNext`, 최대 100개를 사용한다. cursor는 동일한 필터 안에서만 재사용하며 필터가 달라지면 버린다. 동시 수정 시 위치가 이동하는 live 목록이며 과거 스냅샷을 보장하지 않는다.

실행 중인 백엔드의 `/api-docs`가 계약 원본이다. 프론트의 `npm run api:update`는 해당 명세를 내려받아 `src/shared/api/openapi.json` 및 `schema.d.ts`를 생성한다. `API_SCHEMA_URL`로 주소를 지정할 수 있다. `npm run api:check`는 저장된 명세와 타입 간 일치를 검사하며 CI에도 포함했다.

## 예약 멱등성

참가/취소/마감은 회원·작업·모임·키로 성공 응답을 저장한다. 같은 payload는 당시 응답을 반환하고 다른 payload는 `409 IDEMPOTENCY_PAYLOAD_MISMATCH`다. 취소 또는 모임 삭제 후 과거 참가 요청을 재전송해도 새 참가가 생기지 않는다. 키 비교는 대소문자를 구분한다. 오류 트랜잭션은 결과를 저장하지 않는다.

`expires_at`은 30일 후로 기록하지만 자동 삭제 작업은 이번 변경에 포함하지 않는다. 기록이 남아 있는 동안 기존 키의 결과를 유지한다. 보관 정책을 추가할 때 클라이언트 재시도 보장 기간과 함께 정의해야 한다.

## 비동기 처리와 장애 복구

예약 변경과 수신자별 outbox 이벤트가 같은 트랜잭션에 커밋된다. 기본 5초 주기의 DB relay가 `FOR UPDATE SKIP LOCKED`로 최대 100개를 처리한다. 알림에는 event ID unique 제약이 있다. 새 브로커 운영 없이 사이트 알림함을 먼저 제공한다.

잘못된 payload나 데이터 제약 위반은 5회 후 격리한다. 다음 조회로 격리 이벤트를 찾고 원인을 수정한 뒤 `attempts`를 초기화하여 재처리할 수 있다. 사용자 정보가 들어 있을 수 있으므로 이벤트 payload를 일반 로그에 출력하지 않는다.

```sql
SELECT id, event_type, attempts, occurred_at
FROM outbox_event
WHERE published_at IS NULL AND attempts >= 5;
```

연결 장애·데드락처럼 트랜잭션 전체가 실패하는 경우 다음 스케줄에서 재시도한다. 알림 저장과 published 표시가 같은 트랜잭션이므로 중간 장애에도 이중 발송하지 않는다. 대기 승계는 기본 10초 주기이며 취소 직후에도 실행한다.

## 스키마와 호환성

V17–V22는 멱등 키 collation 변경, 알림함, 예약 ready, 대기열, 템플릿, 차단·피드백·신고 테이블을 추가한다. 기존 응답 DTO나 경로를 삭제하지 않았다. 프론트에 새 API가 필요하므로 백엔드 마이그레이션 적용 후 프론트를 반영한다. 실제 서비스 배포는 수행하지 않았다.

캘린더 파일은 종료 시간을 임의 생성하지 않는다. 다운로드 후 모임 변경과 자동 동기화되지 않는다. 알림함과 내 모임에서 최신 일정을 확인해야 한다.

## 검증

MariaDB Testcontainers에서 회원별 목록 분리, 시간/커서 경계, 예약 재전송, 알림 중복·격리, 준비 상태 권한, 대기 우선권/만료/취소, 추천 공석, 템플릿 생성·재시도·소유권, 양방향 차단 및 참가 기록 기반 신고를 검증한다. 기존 60개 동시 예약 테스트도 포함한다.

Docker 29 + Colima 로컬 검증에서는 다음 설정을 사용했다. RYUK 비활성화 시 테스트 컨테이너가 정상 종료되는지 확인한다.

```bash
DOCKER_HOST=unix://$HOME/.colima/default/docker.sock \
TESTCONTAINERS_RYUK_DISABLED=true JAVA_TOOL_OPTIONS=-Dapi.version=1.44 \
./gradlew test checkstyleMain checkstyleTest
```

격리된 MariaDB에 V22까지 적용하여 `JOOQ_CODEGEN=true ./gradlew jooqCodegen compileJava`를 수행했다. 생성물은 build 아래에 두고 Checkstyle은 손으로 작성한 소스에만 적용한다.
