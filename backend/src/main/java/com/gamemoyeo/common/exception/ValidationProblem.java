package com.gamemoyeo.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "입력 검증 실패 응답. fieldErrors.field는 요청 JSON의 항목 경로입니다.")
public record ValidationProblem(
    String type, String title, int status, String detail, String instance,
    String code, String traceId, List<FieldError> fieldErrors
) {
    public record FieldError(
        @Schema(example = "startsAt") String field,
        @Schema(example = "시작 시간은 현재보다 10분 넘게 뒤로 설정해 주세요.") String reason
    ) {
    }
}
