# Riot 계정 연결

기존 게임모여 로그인 후 `/account`에서 Riot 계정을 연결·조회·해제합니다.
이 기능은 Riot 계정으로 게임모여에 신규 가입/로그인하는 기능과 별개입니다.
리그 오브 레전드·VALORANT 전적, 랭크, 게임 보유 여부는 조회하거나 인증하지 않습니다.

## 운영 활성화

Riot 운영 API 키 승인과 RSO 클라이언트 발급이 선행되어야 합니다.
일반 `RGAPI-...` 키는 RSO Client ID/Secret을 대체하지 않습니다.
현재 구현은 **Client Secret Basic** 방식의 RSO 클라이언트를 대상으로 합니다.
Private Key JWT 방식으로 발급된 클라이언트는 별도 구현이 필요합니다.

등록할 Redirect URI:

```text
https://gamemoyeo.noroo.kr/api/v1/riot/callback
```

서버의 비공개 환경 파일 `~/.config/game-moyeo/container.env`에 설정합니다.

```dotenv
RIOT_RSO_ENABLED=true
RIOT_RSO_CLIENT_ID=발급된_ID
RIOT_RSO_CLIENT_SECRET=발급된_Secret
RIOT_RSO_CALLBACK_URI=https://gamemoyeo.noroo.kr/api/v1/riot/callback
RIOT_RSO_FRONTEND_URI=https://gamemoyeo.noroo.kr/riot/callback
```

기본값은 `RIOT_RSO_ENABLED=false`이며 자격 증명 없이 앱을 실행할 수 있습니다.
활성화 시 자격 증명과 고정 HTTPS 콜백 주소가 없으면 서버 시작을 거부합니다.
Client ID/Secret을 프론트 환경 변수나 Git에 저장하지 않습니다.
프론트·API·콜백을 같은 origin으로 서비스해야 합니다. 로컬 연동 테스트에도 HTTPS가 필요합니다.
프론트와 백엔드를 함께 배포하며, Flyway V9가 `riot_account` 테이블을 추가합니다.
기존 테이블과 API 계약은 유지합니다. 운영 적용 시 기존 배포 스크립트의 DB 백업 절차를 사용합니다.

## 인증 흐름과 저장 정보

1. 로그인된 회원이 연결 버튼을 누르면 빈 인증 창을 열고 JWT를 포함한 POST로 시작합니다.
2. 서버가 무작위 state, 브라우저 확인값, PKCE verifier를 생성해 Redis에 5분간 저장합니다.
   브라우저 확인 쿠키는 Secure, HttpOnly, SameSite=Lax이며 콜백 경로로 제한됩니다.
3. Riot 공식 authorize 창으로 이동합니다. `openid`, authorization code, S256 PKCE를 사용합니다.
4. 콜백의 state와 브라우저 확인값을 검증하고 Redis의 state를 원자적으로 한 번만 소비합니다.
5. 서버가 code를 교환하고 RSO access token으로 ASIA의 `/riot/account/v1/accounts/me`를 호출합니다.
   사용자 입력 닉네임 조회 결과나 검증하지 않은 ID token을 소유권 증거로 사용하지 않습니다.
6. 검증된 계정과 시작 회원 ID를 담은 일회용 완료 코드를 Redis에 60초간 저장합니다.
7. 인증 창은 같은 origin의 원래 창에 완료 코드만 전달합니다. 부모 창은 origin과 source를 확인합니다.
8. 원래 창의 JWT로 완료 API를 호출합니다. 시작 회원과 일치할 때만 코드를 소비하고 DB에 연결합니다.

Riot access/refresh token은 DB·Redis·브라우저에 저장하지 않습니다. API 응답 본문이나 인증 코드는
로그에 기록하지 않습니다. 완료 코드는 URL에서 즉시 제거하고 콜백 응답은 no-store/no-referrer입니다.
메인 창을 닫거나 다른 회원으로 로그인하면 기존 완료 코드를 그 회원에게 연결할 수 없습니다.
동시 인증 창은 마지막 시작 요청의 브라우저 쿠키만 유효하므로 실패한 창에서는 다시 시작해야 합니다.
DB에는 회원 ID, PUUID, gameName, tagLine, 연결 시각만 저장합니다.
회원마다 Riot 계정 하나, Riot 계정마다 회원 하나만 허용합니다. 다른 계정으로 교체하려면 먼저 해제합니다.
동일 계정 재인증은 Riot ID를 갱신하며, 연결 해제는 반복해도 안전합니다.
인증 상태·완료 코드는 일회성이므로 실패나 완료 응답 유실 시 상태를 조회한 후 필요하면 다시 시작합니다.

## REST 계약

Springdoc가 아래 컨트롤러/DTO에서 OpenAPI를 생성합니다. 운영 API 문서 공개 설정은 유지합니다.
콜백을 제외한 모든 요청은 게임모여 Bearer JWT가 필요하며, 회원 ID는 JWT subject에서만 얻습니다.

| 메서드 | 경로 | 응답 |
| --- | --- | --- |
| GET | `/api/v1/members/me/riot` | 200 `{enabled, linked, riotId}`; 미연결이면 riotId=null |
| POST | `/api/v1/members/me/riot/authorization` | 200 `{authorizationUrl}` 및 브라우저 확인 쿠키 |
| GET | `/api/v1/riot/callback` | Riot의 code/state 또는 error 수신; 프론트 콜백으로 302 |
| POST | `/api/v1/members/me/riot` | 요청 `{code}`; 연결 성공 204 |
| DELETE | `/api/v1/members/me/riot` | 연결 해제 204 |

대표 오류: `401` 인증 필요, `400 RIOT_LINK_EXPIRED` 만료·재사용·회원 불일치,
`409 RIOT_ACCOUNT_ALREADY_LINKED` 중복/다른 계정 연결,
`503 RIOT_LINK_UNAVAILABLE` 승인 대기 또는 기능 비활성,
`502 RIOT_AUTH_FAILED` Riot 통신·인증 실패. REST 오류는 공통 Problem Details 형식입니다.
외부 콜백 오류는 원문 대신 `error=failed`만 전달합니다.

## 검증 및 실제 승인 후 확인

자동 검증은 모의 Riot 응답, 실제 MariaDB·Redis Testcontainers, Playwright 팝업 흐름을 사용합니다.
실제 RSO 클라이언트의 승인된 scope·PKCE 지원·팝업 정책과 실계정 인증은 승인 후 검증해야 합니다.
활성화 전에 정상 연결, 로그인 취소, 기존 연결 충돌, 만료 후 재시도, 모바일 인증 창 복귀를 확인합니다.
개인정보처리방침에 위 저장 정보·이용 목적·연결 해제를 반영하고 Riot 신청 정보와 사용자 흐름을 일치시킵니다.

공식 근거: [Riot RSO](https://developer.riotgames.com/docs/lol#rso-integration),
[RSO 승인 FAQ](https://developer.riotgames.com/docs/faqs).
