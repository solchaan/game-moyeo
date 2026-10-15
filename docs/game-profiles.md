# 게임별 기본 포지션

- 게임 계정 화면의 롤 프로필에서 주·부 포지션을 저장한다. Riot 계정 연결 없이도 사용할 수 있다.
- 주 포지션은 TOP, JUNGLE, MID, ADC, SUPPORT, ANY 또는 미설정이다. 부 포지션은 선택 사항이며 주 포지션과 같을 수 없다. 주 포지션이 없으면 부 포지션도 없다.
- GET/PUT `/api/v1/members/me/games/{gameId}/profile`: 인증된 사용자 자신의 게임 프로필만 조회·수정한다. 현재는 league-of-legends만 지원한다.
- GET `/api/v1/meetups/{meetupId}/participation/preferences`: 서버가 맵에 따른 포지션 선택 가능 여부와 프로필 기본값을 반환한다. 활성 소환사의 협곡 맵에서만 사용한다.
- POST `/api/v1/meetups/{meetupId}/reservations`: 선택한 `primaryPosition`, `secondaryPosition`을 신청에 저장한다. 본문 생략 시 서버가 프로필 기본값을 적용하며, 둘 다 null을 명시하면 미설정으로 신청한다.
- 신청 시 수정한 값은 프로필을 변경하지 않는다. 취소 후 재신청 시 새 신청 값을 저장한다. 포지션은 선호 정보이며 역할별 좌석 배정이나 자동 매칭을 의미하지 않는다.
- V13은 사용자·게임별 프로필 테이블과 예약의 포지션 열을 추가한다. 기존 사용자와 예약은 미설정으로 유지한다. V11/V12를 포함한 순차 마이그레이션 후 프론트엔드와 백엔드를 함께 배포한다.
