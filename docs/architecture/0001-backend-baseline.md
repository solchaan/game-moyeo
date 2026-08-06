# ADR 0001: Backend architecture baseline

- Status: Accepted
- Date: 2026-08-06

## Decision

게임 모임 플랫폼의 백엔드는 Clean Architecture를 적용한 Spring Boot 모듈러 모놀리스로 시작한다. 업무 변경은 JPA, 복잡한 조회는 jOOQ를 사용한다. MariaDB 스키마의 단일 원본은 Flyway migration이며 jOOQ 생성물과 JPA 매핑은 해당 스키마를 따른다.

예약 수용 가능 여부는 MariaDB의 조건부 `UPDATE`와 unique/check constraint로 원자적으로 판정한다. 멱등 키로 클라이언트 재시도를 안전하게 만들고, 후속 작업은 transactional outbox로 전달한다.

SPA에는 `/api/v1` JSON REST API와 OpenAPI 문서를 제공한다. 모든 오류는 전역 예외 변환을 통해 Problem Details 형식으로 반환한다.

## Why

모듈러 모놀리스는 초기 운영 복잡도를 낮추면서 도메인 경계를 유지한다. JPA는 애그리게이트 쓰기에, jOOQ는 복잡한 SQL 조회에 각각 강점이 있다. DB 조건부 갱신은 여러 API 인스턴스에서도 단일 정합성 지점을 제공하며 Redis 분산 락의 만료·유실 문제에 좌석 확정을 의존하지 않는다.

## Consequences

- Flyway migration과 생성된 jOOQ 타입 사이의 drift를 CI에서 검사해야 한다.
- domain/JPA/read model 간 매핑 코드가 생기지만 계층 간 결합과 API 누출을 줄인다.
- 알림과 통계는 eventual consistency를 허용한다. 예약 확정과 정원 계산만 동일 DB 트랜잭션에서 strong consistency를 보장한다.
- 트래픽 증가 시 조회 replica, Redis cache, broker consumer를 독립적으로 확장할 수 있다.

## Initial API surface

```text
GET    /api/v1/games
GET    /api/v1/meetups?gameId=&cursor=&size=
POST   /api/v1/meetups
GET    /api/v1/meetups/{meetupId}
POST   /api/v1/sessions/{sessionId}/reservations
DELETE /api/v1/sessions/{sessionId}/reservations/{reservationId}
GET    /api/v1/me/reservations?cursor=&size=
```

`POST /reservations`는 `Idempotency-Key` header를 필수로 받고 예약 성공 시 `201`, 동일 요청 재생 시 동일 응답, 중복/정원 충돌 시 `409`를 반환한다.
