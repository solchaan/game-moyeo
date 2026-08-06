# gameMoyeo AI Engineering Harness

이 파일은 이 저장소에서 작업하는 AI 에이전트의 상시 시스템 프롬프트다. 모든 변경은 아래 규칙을 따른다.

## 1. 제품 목표

여러 온라인 게임의 모임을 검색하고, 모임을 만들고, 정원이 있는 세션을 예약·취소하는 REST API를 만든다. 초기에는 모듈러 모놀리스로 배포하되 모듈 경계와 이벤트 계약을 지켜 향후 독립 서비스로 분리할 수 있어야 한다.

핵심 도메인은 `member`, `game`, `meetup`, `reservation`, `notification`이다. 게임별 차이는 `game`의 메타데이터와 정책으로 표현하고 게임마다 동일한 예약 로직을 복제하지 않는다.

## 2. 기준 기술 스택

- Java 21, Spring Boot 3.x, Gradle
- MariaDB(InnoDB), Spring Data JPA/Hibernate: 애그리게이트 저장과 변경
- Flyway: 실행 순서가 보장되는 SQL 마이그레이션과 스키마 버전 관리
- jOOQ: Flyway가 만든 스키마에서 타입 안전 코드를 생성하고 복잡한 조회/read model에 사용
- Redis: 캐시, rate limit, 짧은 수명의 분산 조정에만 사용. 예약 정합성의 유일한 근거로 사용하지 않는다.
- OpenAPI 3, JSON REST API, Testcontainers, JUnit 5

주의: jOOQ 자체는 DB 마이그레이션 도구가 아니다. DB 형상은 `src/main/resources/db/migration`의 Flyway SQL이 단일 원본이며, jOOQ 코드는 그 형상으로부터 생성한다. JPA의 `ddl-auto`는 `validate`만 허용한다.

## 3. Clean Architecture 의존성 규칙

각 도메인 모듈은 다음 패키지 구조를 사용한다.

```text
com.gamemoyeo.<domain>
├── domain/          # 엔티티, 값 객체, 도메인 정책·이벤트; Spring/JPA 비의존
├── application/
│   ├── port/in/     # 유스케이스 인터페이스와 command/query
│   ├── port/out/    # 저장소, clock, 이벤트 발행 등 포트
│   └── service/     # 유스케이스 구현과 트랜잭션 경계
└── adapter/
    ├── in/web/      # REST controller, request/response DTO, mapper
    └── out/         # JPA, jOOQ, Redis, 외부 API 어댑터
```

의존성 방향은 `adapter -> application -> domain`뿐이다. 도메인은 Spring, JPA, HTTP DTO를 import하지 않는다. JPA persistence model과 domain model은 분리하고 어댑터에서 매핑한다. 컨트롤러는 유스케이스만 호출하며 repository를 직접 호출하지 않는다. 모듈 간 직접 테이블 접근 대신 공개 유스케이스 또는 명시된 이벤트 계약을 사용한다.

## 4. JPA, jOOQ, 트랜잭션

- 쓰기와 단순 애그리게이트 조회는 JPA repository adapter가 담당한다.
- 조인·집계·검색·목록 read model은 jOOQ query adapter가 담당한다.
- API DTO, domain model, JPA entity, jOOQ record를 서로 노출하지 않는다.
- 모든 쓰기 유스케이스의 application service에 트랜잭션 경계를 둔다. 외부 네트워크 호출은 DB 트랜잭션 안에서 실행하지 않는다.
- N+1을 테스트와 SQL 관찰로 방지하고, 목록 API에 반드시 pagination과 deterministic sort를 둔다.
- 마이그레이션은 수정하지 않고 새 버전을 추가한다. 배포 중 호환성을 위해 expand/migrate/contract 순서를 따른다.

## 5. REST API 계약

- 리소스 중심 URI와 적절한 HTTP method/status를 사용하고 `/api/v1`으로 버전 관리한다.
- SPA가 소비할 request/response DTO만 공개한다. Entity를 JSON으로 직렬화하지 않는다.
- 생성은 `201 + Location`, 비동기 접수는 `202`, 삭제 성공은 `204`를 우선한다.
- 목록은 cursor pagination을 우선하며 `items`, `nextCursor`, `hasNext`를 반환한다.
- 쓰기 재시도를 위해 `Idempotency-Key`를 지원한다. 동일 사용자·키·operation은 동일 결과를 반환하고 payload가 다르면 `409`를 반환한다.
- OpenAPI 명세와 구현을 함께 변경한다. CORS 허용 origin은 설정값의 명시적 allow-list로 제한한다.

### 공통 오류

모든 endpoint는 예외를 RFC 9457 Problem Details(`application/problem+json`)로 변환하는 전역 `@RestControllerAdvice`를 통과한다.

```json
{
  "type": "https://api.gamemoyeo.com/problems/reservation-conflict",
  "title": "Reservation conflict",
  "status": 409,
  "detail": "The session has no remaining capacity.",
  "instance": "/api/v1/sessions/123/reservations",
  "code": "RESERVATION_CAPACITY_EXCEEDED",
  "traceId": "...",
  "fieldErrors": [{"field": "partySize", "reason": "must be positive"}]
}
```

예외 매핑: 검증 실패 `400`, 인증 실패 `
401`, 권한 없음 `403`, 리소스 없음 `404`, 중복·상태 충돌 `409`, 사전조건 실패 `412`, rate limit `429`, 예상하지 못한 오류 `500`. 내부 예외명, SQL, stack trace, 개인정보는 응답하지 않는다. `500`은 서버 로그에 traceId와 stack trace를 기록한다. 알려진 업무 실패는 안정적인 `code`를 갖는 application/domain exception으로 표현한다.

## 6. 동시 예약 정합성

MariaDB가 예약 결과의 source of truth다. 한 세션 예약은 다음 순서로 처리한다.

1. 인증 사용자와 `Idempotency-Key`를 검증한다.
2. 짧은 DB 트랜잭션을 시작한다.
3. 중복 예약은 `UNIQUE(session_id, member_id)`로 차단한다.
4. 정원 확보는 원자적 조건부 갱신으로 수행한다.

```sql
UPDATE meetup_session
SET reserved_count = reserved_count + :partySize,
    version = version + 1
WHERE id = :sessionId
  AND status = 'OPEN'
  AND reserved_count + :partySize <= capacity;
```

영향 행이 0이면 최신 상태를 확인해 `404`, `409 SESSION_CLOSED`, `409 RESERVATION_CAPACITY_EXCEEDED` 중 하나로 반환한다. 성공한 경우 reservation을 삽입하고 같은 트랜잭션에 outbox event를 기록한다. 취소는 상태 조건부 갱신 후 reserved count를 감소시키며 음수가 되지 않는 DB 제약을 둔다. deadlock/lock-timeout은 제한된 횟수만 jitter를 포함해 재시도하고 소진 시 `409` 또는 `503`을 반환한다.

단일 좌석처럼 경합이 극심한 별도 흐름은 `SELECT ... FOR UPDATE`를 사용할 수 있으나 lock 순서를 고정하고 트랜잭션을 짧게 유지한다. Redis lock만으로 좌석을 확정하지 않는다. 동시성 테스트는 실제 MariaDB Testcontainer에 동시 요청을 보내 `reserved_count <= capacity`, 중복 없음, 예약 합계 일치를 검증한다.

## 7. 대용량 트래픽

- API 서버를 stateless하게 유지하고 수평 확장한다. 세션·idempotency·rate-limit 상태는 외부 저장소에 둔다.
- CDN에서 정적 SPA와 이미지 제공, reverse proxy에서 압축·connection limit·request size limit을 적용한다.
- 게임/모임 상세과 인기 목록은 Redis cache-aside로 캐시하고 TTL에 jitter를 둔다. 갱신 후 event 기반 무효화하며 cache stampede를 방지한다.
- DB는 커넥션 풀 상한을 인스턴스 수와 DB 한도에 맞춘다. 적절한 복합 인덱스와 cursor pagination을 사용한다. 복제 지연을 허용할 수 있는 조회만 read replica로 보낸다.
- 알림, 검색 색인, 통계는 transactional outbox에서 broker consumer로 비동기 처리한다. consumer는 idempotent해야 하며 retry/DLQ를 둔다.
- 사용자/IP별 rate limit, timeout, circuit breaker, bulkhead와 graceful degradation을 적용한다.
- 예약 쓰기 요청은 무제한 queueing하지 않는다. load shedding과 `429/503 + Retry-After`로 과부하를 명시한다.
- p95/p99 latency, error rate, throughput, DB pool/lock wait, cache hit, queue lag을 관찰하고 traceId를 로그·메트릭·trace에 연결한다.

## 8. 보안과 테스트 완료 조건

- 인증 주체의 ID는 request body가 아니라 security context에서 얻는다. 객체 단위 권한을 application layer에서 검사한다.
- 입력 길이·범위·enum을 검증하고, jOOQ bind parameter/JPA parameter binding만 사용한다.
- secret 및 개인정보를 코드·로그에 남기지 않는다.
- 변경마다 domain unit test, application use-case test, web contract test를 작성한다.
- DB 관련 변경은 MariaDB Testcontainers integration test와 Flyway migration test를 포함한다.
- 예약 변경은 중복, 정원 경계, 취소, idempotency, 50개 이상의 동시 요청을 검증한다.
- ArchUnit으로 의존성 방향을 강제한다.
- 완료 전 `./gradlew clean test`와 jOOQ code generation/compile을 실행하고 결과를 보고한다.

## 9. AI 작업 절차

1. 요청을 도메인 규칙, API 계약, 데이터 변경, 비기능 요구로 분해한다.
2. 관련 코드와 마이그레이션을 먼저 읽고 불명확한 정책은 명시적 가정으로 남긴다.
3. domain/application port부터 설계하고 adapter를 마지막에 연결한다.
4. 스키마 변경이 있으면 Flyway SQL, jOOQ 생성, JPA 매핑, 테스트를 한 변경 단위로 처리한다.
5. 실패·재시도·동시성·권한 경로를 happy path와 함께 구현한다.
6. 작은 변경 단위로 검증하고 최종 보고에 변경 파일, API/DB 호환성, 실행한 테스트, 남은 위험을 쓴다.
7. 사용자 추가 요청 사항이 코드 재사용, 리팩토링 가능한 구조라면 사용자에게 즉시 문의한다.

아키텍처 규칙을 어겨야 한다면 조용히 우회하지 말고 이유와 대안을 ADR로 남긴다.
