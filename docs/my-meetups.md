# 내 모임과 일정

`GET /api/v1/members/me/meetups?filter=UPCOMING&size=20&cursor=...`

인증 주체의 모임만 반환한다. `memberId` 입력은 사용하지 않는다.

- UPCOMING: 방장이거나 현재 참가 확정인 모임. 시작 시각 이상, 삭제 제외. 시작 시각/ID 오름차순.
- HOSTED: 내가 만든 모임. 삭제된 모임도 취소된 기록으로 표시. 시작 시각/ID 내림차순.
- PAST: 방장이거나 현재 참가 확정인 모임 중 시작 시각이 지난 모임. 실제 종료를 의미하지 않는다. 시작 시각/ID 내림차순.
- 응답: `items`, `nextCursor`, `hasNext`. cursor는 응답을 그대로 URL 인코딩하여 전달한다.
- `canOpen`, `canExportCalendar`는 서버가 판정한다. 삭제된 모임은 상세 링크와 캘린더 저장을 제공하지 않는다.
- 취소는 상세 화면의 기존 참가 취소 흐름을 사용한다.
- 캘린더는 UTC DTSTART만 제공한다. 종료 시간을 임의로 생성하지 않는다. 다운로드한 일정은 이후 수정과 자동 동기화되지 않는다.

새 테이블이나 기존 API 변경 없이 추가된다. jOOQ 조회에서 회원 조건을 pagination 전에 적용한다.
