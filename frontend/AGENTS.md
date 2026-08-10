# gameMoyeo Frontend AI Engineering Harness

이 파일은 `frontend/` 아래에서 작업하는 AI 에이전트의 상시 프롬프트다. 루트 `AGENTS.md`의 API·보안·품질 규칙을 함께 따르며, 충돌하면 더 엄격한 규칙을 적용한다.

## 1. 제품과 사용자 목표

사용자가 원하는 게임, 모드, 시간, 티어, 역할과 플레이 성향에 맞는 모임을 빠르게 찾고 안전하게 참가하도록 돕는다. 관리자는 게임과 게임별 선택 항목을 개발자 도움 없이 관리할 수 있어야 한다.

화면의 목표는 기능 수가 아니라 다음 사용자 과업의 성공률이다.

1. 조건에 맞는 모임을 찾는다.
2. 모집 조건을 충분히 이해한다.
3. 중복이나 정원 초과 없이 참가한다.
4. 부담 없이 모임을 만들고 수정한다.
5. 신청·승인·취소 상태와 다음 행동을 즉시 이해한다.

## 2. 기준 기술 스택

- React, TypeScript strict mode, Vite
- React Router: route와 접근 제어
- TanStack Query: 모든 server state 조회·mutation·cache 관리
- Zustand: 인증 UI 상태 등 작은 client state만 관리
- React Hook Form + Zod: 입력 UX와 클라이언트 형식 검증
- Tailwind CSS + shadcn/ui: 디자인 토큰 기반 UI
- date-fns 또는 Temporal polyfill: 표시용 날짜 처리
- Vitest + React Testing Library + MSW: 단위·통합 테스트
- Playwright: 핵심 사용자 여정 E2E 테스트
- ESLint + Prettier: 정적 분석과 일관된 포맷
- Storybook: 공통 컴포넌트와 상태 문서화

라이브러리를 추가할 때는 기존 도구로 해결할 수 없는 문제인지 확인한다. 동일 목적의 상태·폼·날짜·HTTP 라이브러리를 중복 도입하지 않는다.

## 3. 서버 중심 계산 원칙

업무 규칙과 파생 값의 단일 원본은 백엔드다. 프론트엔드는 서버 결과를 임의로 재계산하거나 판정하지 않는다.

### 프론트에서 계산하지 않는 값

- 남은 모집 인원, 예약 가능 여부, 대기 순번
- 모집 상태, 마감 여부, 참가·취소 가능 여부
- 티어 범위 충족 여부, 역할별 잔여 인원
- 가격·포인트·패널티·노쇼 횟수
- 권한, 관리자 여부, 게시글 수정·삭제 가능 여부
- 시간대 변환이 완료된 업무 마감 판정
- cursor, 다음 페이지 존재 여부
- 서버에서 정한 정렬 점수, 추천 점수, 인기 점수

API는 가능하면 다음처럼 화면에 필요한 파생 상태를 제공해야 한다.

```json
{
  "capacity": 5,
  "reservedCount": 3,
  "remainingCapacity": 2,
  "recruitmentStatus": "OPEN",
  "canReserve": true,
  "canCancel": false,
  "canEdit": false,
  "availableActions": ["RESERVE"]
}
```

백엔드가 필요한 값을 제공하지 않으면 프론트에 임시 업무 계산을 추가하지 않는다. API 계약 변경을 요청하고, 불가피한 표시용 계산은 순수 함수로 격리해 근거와 제거 조건을 남긴다.

### 프론트에서 허용하는 계산

- 레이아웃, 애니메이션, 목록 가상화 등 표현 계산
- 사용자의 로컬 시간대에 맞춘 날짜 포맷
- 입력 글자 수와 즉각적인 형식 안내
- 서버 요청 전 비어 있는 값, 문자열 길이, 명백한 형식 오류 확인
- progress bar처럼 서버가 준 분자·분모를 시각화하는 계산

클라이언트 검증은 UX 보조일 뿐이며 서버 검증을 대체하지 않는다.

## 4. API 계층과 타입

```text
src/
├── app/                 # router, providers, global error boundary
├── pages/               # route 단위 조합
├── features/            # meetup, reservation, auth 등 사용자 행동
├── entities/            # API resource 표시 모델
├── shared/
│   ├── api/             # HTTP client, generated schema, Problem Details
│   ├── ui/              # 디자인 시스템 컴포넌트
│   ├── lib/             # 날짜·접근성·테스트 유틸
│   └── config/          # 환경설정
└── assets/
```

- 페이지와 컴포넌트에서 `fetch`/Axios를 직접 호출하지 않는다.
- OpenAPI에서 TypeScript API 타입을 생성하고 임의의 중복 DTO를 만들지 않는다.
- API 응답과 form model이 다르면 feature mapper에서 명시적으로 변환한다.
- 서버에서 nullable인 값을 억지로 non-null assertion(`!`)하지 않는다.
- 응답 필드를 추측하거나 여러 이름을 fallback으로 읽지 않는다.
- 모든 요청에는 timeout과 취소 신호를 적용한다.
- 목록은 서버의 `items`, `nextCursor`, `hasNext`를 그대로 사용한다.
- URL query string을 검색·필터·정렬 상태의 공유 가능한 원본으로 사용한다.

## 5. Server state와 client state

TanStack Query가 API 데이터의 단일 캐시다. API 데이터를 Zustand, Context 또는 component state에 복제하지 않는다.

- query key factory를 feature별로 정의한다.
- mutation 성공 후 서버 응답으로 cache를 갱신하거나 필요한 key만 invalidate한다.
- optimistic update는 되돌리기가 명확하고 충돌 위험이 낮은 기능에만 사용한다.
- 예약처럼 동시성 충돌이 중요한 mutation은 서버 응답을 기다리고 `409`를 정확히 표시한다.
- 화면 재진입 시 무조건 refetch하지 않고 데이터 성격에 맞는 `staleTime`을 정한다.
- 검색 입력은 debounce하되 이미 시작한 이전 요청을 취소한다.
- 무한 스크롤에도 키보드 사용자를 위한 명시적 “더 보기” 대안을 제공한다.

Zustand에는 theme, dismissible UI, 인증 완료 전 redirect 목적지처럼 서버 데이터가 아닌 최소 상태만 둔다.

## 6. 인증과 보안

- Kakao/Naver 로그인은 백엔드의 `/oauth2/authorization/{provider}`로 이동한다.
- callback의 일회용 code는 즉시 `/api/v1/auth/token`으로 교환하고 URL에서 제거한다.
- OAuth provider access token을 브라우저에서 보관하지 않는다.
- access token은 가능한 한 메모리에만 보관한다.
- refresh token은 백엔드가 HttpOnly, Secure, SameSite cookie로 전환하는 것을 최종 목표로 한다. 현재 JSON 반환 방식에서는 localStorage 저장을 기본값으로 채택하지 말고 보안 ADR 없이는 구현하지 않는다.
- 인증 실패 `401`과 권한 부족 `403`을 구분한다.
- 화면에서 버튼을 숨겨도 보안 경계가 되지 않는다. 서버 권한 검사가 최종 기준이다.
- 외부 URL은 allow-list 또는 안전한 URL 검증을 거치며 `target="_blank"`에는 `rel="noopener noreferrer"`를 사용한다.
- 사용자 입력 HTML을 그대로 렌더링하지 않는다. `dangerouslySetInnerHTML`은 승인된 sanitizer 없이는 금지한다.
- secret을 `VITE_` 환경변수에 넣지 않는다. `VITE_` 값은 모두 브라우저에 공개된다.

## 7. 오류와 복구 UX

모든 API 오류는 `application/problem+json`의 `code`, `detail`, `fieldErrors`, `traceId`를 공통 파서로 처리한다.

- `400`: field error를 해당 입력과 오류 요약 영역에 함께 표시
- `401`: 로그인 유도 후 원래 화면으로 복귀
- `403`: 권한이 부족한 이유와 가능한 다음 행동 안내
- `404`: 사라진 리소스와 목록으로 돌아가기 제공
- `409`: 최신 상태를 다시 불러오고 구체적인 충돌 사유 표시
- `412`: 오래된 편집 상태임을 알리고 비교 또는 새로고침 제공
- `429`: `Retry-After` 동안 버튼 비활성화와 남은 시간 안내
- `500/503`: 재시도 버튼, 상태 보존, traceId 기반 문의 정보 제공

예약 충돌을 “알 수 없는 오류”로 표시하지 않는다. 예:

```text
방금 마지막 자리가 예약됐어요.
현재 모집 상태를 새로 불러왔습니다. 대기 신청이 가능하면 이어서 신청할 수 있어요.
```

toast만으로 중요한 오류를 알리지 않는다. 사용자가 행동해야 하는 오류는 관련 영역에 지속적으로 표시한다.

## 8. 사용자 친화적 UI/UX

### 공통 원칙

- 한 화면의 primary action은 하나로 명확히 한다.
- 사용자가 입력한 값과 필터는 오류·뒤로가기·로그인 왕복 후에도 보존한다.
- destructive action은 대상과 영향을 명확히 보여주며 필요할 때만 확인 dialog를 사용한다.
- 시스템 용어보다 사용자 언어를 사용한다. `409`, `MODE`, `FIRST_COME`를 그대로 노출하지 않는다.
- 게임 이미지나 색상에만 의존하지 않고 텍스트 이름을 함께 제공한다.
- 모바일 360px부터 데스크톱까지 반응형으로 설계한다.
- skeleton은 실제 레이아웃과 유사하게 만들고 layout shift를 최소화한다.
- 빈 화면은 이유, 예시, 다음 행동을 포함한다.
- 성공 메시지는 결과와 후속 행동을 말한다.

### 모임 탐색

- 게임, 모드, 날짜, 시간대, 티어, 역할 필터를 우선 제공한다.
- 적용 중인 필터를 removable chip으로 표시하고 전체 초기화를 제공한다.
- 필터 변경 결과 수는 서버가 제공한 값을 표시한다.
- 모집 마감, 남은 자리, 음성 채팅, 승인 방식을 카드에서 비교 가능하게 표시한다.
- 색상만으로 OPEN/CLOSED 상태를 표현하지 않는다.
- 목록 위치와 scroll을 상세 화면 왕복 후 복원한다.

### 모임 작성

- 긴 단일 form 대신 게임 선택 → 조건 → 일정·인원 → 확인 단계로 나눈다.
- 선택한 게임의 활성 옵션만 서버에서 받아 표시한다.
- 날짜와 시간은 사용자의 timezone을 명시하고 최종 확인 단계에 절대 시각을 보여준다.
- 서버가 계산한 모집 요약을 제출 전에 preview한다.
- 임시 저장이 있다면 서버 draft API를 사용한다. localStorage를 업무 데이터 원본으로 사용하지 않는다.
- 제출 중 중복 클릭을 막고 `Idempotency-Key`를 요청마다 생성한다.

### 예약

- 예약 버튼에는 현재 가능한 행동을 명확한 동사로 표시한다.
- 서버 응답 전 예약 성공처럼 보이게 하지 않는다.
- 성공 후 참가 상태, 일정, 취소 정책, Discord 공개 시점을 한 화면에서 안내한다.
- 중복 요청, 정원 초과, 모집 종료를 각각 다른 메시지로 처리한다.

## 9. 접근성

WCAG 2.2 AA를 목표로 한다.

- 모든 기능을 키보드만으로 사용할 수 있어야 한다.
- semantic HTML을 우선하고 ARIA는 부족한 의미를 보완하는 데만 사용한다.
- form control에는 visible label과 오류 연결을 제공한다.
- modal은 focus trap, 초기 focus, ESC 종료, focus 복귀를 지원한다.
- route 변경과 비동기 결과를 적절한 live region으로 알린다.
- focus indicator를 제거하지 않는다.
- 일반 텍스트 대비 4.5:1 이상을 유지한다.
- touch target은 최소 44x44 CSS pixel을 목표로 한다.
- `prefers-reduced-motion`을 존중한다.
- 이미지에는 목적에 맞는 alt를 제공하고 장식 이미지는 빈 alt를 사용한다.

## 10. 성능

- route 단위 code splitting을 적용한다.
- 초기 화면에 필요 없는 게임 이미지와 dialog를 lazy load한다.
- 이미지 width/height를 지정하고 적절한 포맷·크기를 사용한다.
- 긴 목록은 pagination 우선, 필요할 때 virtualization을 사용한다.
- 불필요한 `useMemo`, `useCallback`, 전역 상태를 성능 해결책처럼 남발하지 않는다.
- 번들 분석 없이 큰 dependency를 추가하지 않는다.
- Core Web Vitals 목표: LCP 2.5초 이하, INP 200ms 이하, CLS 0.1 이하.
- Web Vitals와 API 오류율을 관측하되 개인정보와 token을 로그로 보내지 않는다.

## 11. 디자인 시스템과 반응형

- color, spacing, radius, typography, shadow를 design token으로 관리한다.
- 공통 상태를 `loading`, `empty`, `error`, `disabled`, `success`로 문서화한다.
- button, input, select, combobox, date/time picker, dialog, toast, badge의 API를 일관되게 유지한다.
- 게임별 브랜드 색은 강조에만 사용하고 서비스 전체 접근성 색상 체계를 깨지 않는다.
- 모바일에서는 핵심 행동을 thumb reach에 배치하고, desktop table을 그대로 축소하지 않는다.
- 다크 모드는 token 수준에서 지원하며 개별 컴포넌트에 임의 색상을 하드코딩하지 않는다.

## 12. 국제화와 날짜

- 사용자 노출 문자열을 컴포넌트에 산발적으로 하드코딩하지 않는다.
- 최초 언어는 한국어지만 i18n key 구조를 준비한다.
- 서버와 통신하는 시각은 ISO-8601 UTC를 사용한다.
- 화면에는 사용자의 timezone과 locale로 표시한다.
- “오늘”, “내일”, “3시간 후” 같은 상대 시각에는 정확한 절대 시각을 함께 확인할 수 있게 한다.
- DST와 자정을 프론트 업무 규칙으로 판정하지 않는다.

## 13. 테스트 완료 조건

- pure formatter/mapper/validation unit test
- page와 feature integration test(MSW 사용)
- keyboard navigation과 accessible name 검사
- Problem Details 상태별 UI 테스트
- loading, empty, error, success, slow response 상태 테스트
- Playwright 핵심 여정:
  1. 게임·모드 필터로 모임 검색
  2. 소셜 로그인 callback code 교환
  3. 모임 작성·수정·삭제
  4. 예약 성공
  5. 동시 예약 `409` 복구
- viewport 360px, tablet, desktop 시각 회귀 검사
- 완료 전 typecheck, lint, unit test, production build, E2E smoke를 실행한다.

테스트에서 implementation detail보다 사용자가 보는 role, label, text와 행동을 검증한다.

## 14. AI 작업 절차

1. 요청을 사용자 과업, API 계약, 화면 상태, 접근성, 실패 복구로 분해한다.
2. OpenAPI와 실제 백엔드 응답을 먼저 확인한다.
3. 서버가 제공하지 않는 업무 파생 값이 필요하면 API 변경을 먼저 제안한다.
4. loading, empty, partial, error, success 상태를 happy path와 함께 설계한다.
5. mobile wireflow와 keyboard flow를 먼저 확인한다.
6. shared UI보다 feature 경계를 먼저 잡고 실제 반복이 확인될 때 공통화한다.
7. 구현과 동시에 MSW handler와 사용자 중심 테스트를 작성한다.
8. 최종 보고에 변경 화면, API 의존성, 접근성, 실행한 테스트, 남은 위험을 쓴다.

완료 기준은 “화면이 보인다”가 아니라 사용자가 도움 없이 과업을 완료하고 실패에서 복구할 수 있는 상태다.
