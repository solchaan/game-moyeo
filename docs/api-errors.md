# API 오류와 화면 복구

모든 화면의 TanStack Query 조회·변경 실패는 하단 토스트에서 안내한다. 조회 재시도가 끝난 뒤 한 번 표시하며, 같은 문구는 8초 동안 중복 표시하지 않는다. 최대 3개를 표시하고 10초 후 닫힌다. 마우스나 키보드 포커스가 알림에 있으면 자동 닫기를 중지한다. 네트워크 오류, 인증·권한·충돌·요청 제한·서버 오류는 한국어 안내를 제공한다.

폼은 `FeedbackForm`으로 감싸고 요청 DTO 경로를 `Field name`으로 지정한다. 모임 작성·수정, 로그인·회원가입, 관리자 게임 등록·수정에 적용했다. 예: `startsAt`, `options[0].code`. 서버 오류 항목은 입력란의 `aria-invalid`, `aria-describedby`와 오류 요약에 연결된다. 첫 오류 항목으로 포커스를 옮기고, 항목을 수정하면 해당 오류 표시를 해제한다. 토스트를 닫아도 폼 오류와 입력값은 남는다. `Retry-After`가 있으면 지정 시간 동안 폼을 비활성화하고 남은 시간을 표시한다.

## HTTP 계약

기존 상태 코드와 업무 오류 코드는 유지한다. DB 스키마 변경은 없다. 다음은 `POST /api/v1/meetups`와 `PUT /api/v1/meetups/{meetupId}`의 400 응답 예시다.

```json
{
  "type": "https://api.gamemoyeo.com/problems/invalid-meetup-option",
  "title": "Bad Request",
  "status": 400,
  "code": "INVALID_MEETUP_OPTION",
  "detail": "Meetup must start more than 10 minutes from now.",
  "instance": "/api/v1/meetups",
  "traceId": "example-trace",
  "fieldErrors": [
    {"field": "startsAt", "reason": "시작 시간은 현재보다 10분 넘게 뒤로 설정해 주세요."}
  ]
}
```

- Bean Validation 실패: `VALIDATION_FAILED`, `fieldErrors` 포함.
- JSON 형식 오류: `INVALID_REQUEST_BODY`. Jackson이 식별할 수 있는 필드 경로만 포함하고 입력값이나 내부 예외는 노출하지 않는다.
- 모임 시간·게임·옵션·티어·역할 정원 오류: 기존 `INVALID_MEETUP_OPTION`에 `fieldErrors` 추가.
- `traceId`는 서버에서 제공될 때 문의 번호로 표시한다.

`GlobalExceptionHandler`는 Spring Boot 기본 Problem Details 처리기보다 먼저 실행한다. MVC 예외는 `ResponseEntityExceptionHandler`의 HTTP 상태 매핑을 유지한다. 실제 Spring MVC 테스트에서 `spring.mvc.problemdetails.enabled=true`로 검증한다.

이 변경은 오류 원인을 표시하고 복구를 돕는다. 다른 사용자의 실제 실패 요청 본문은 확보하지 않았으므로 그 사용자의 구체적인 실패 항목까지 확정한 것은 아니다.

## 검증 결과

- 프론트엔드 TypeScript 검사·프로덕션 빌드 통과.
- Playwright 전체 66개 통과(360px 모바일, 태블릿, 데스크톱). 마지막 포커스 보완은 해당 테스트를 세 화면 크기에서 추가 검증.
- `./gradlew clean test checkstyleMain checkstyleTest` 통과: 테스트 34개 통과, Docker 관련 테스트 4개 건너뜀.
- `JOOQ_CODEGEN=true ./gradlew jooqCodegen compileJava`에 해당하는 생성·컴파일 작업 성공. 생성 소스까지 포함한 Checkstyle은 기존 jOOQ 생성 코드의 중괄호 규칙 위반으로 실패했고, clean 이후 애플리케이션 소스 검사는 통과했다.
- 프론트엔드에는 별도 lint/unit-test 스크립트가 없어 TypeScript와 브라우저 통합 테스트로 검증했다.
- 실제 운영 사용자 요청 재현과 운영 배포는 수행하지 않았다. 프론트엔드와 백엔드를 함께 배포해야 오류 항목 전달과 화면 표시가 모두 적용된다.
