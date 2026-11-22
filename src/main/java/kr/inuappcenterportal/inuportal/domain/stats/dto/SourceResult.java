package kr.inuappcenterportal.inuportal.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 데이터 출처 하나의 조회 결과. 한 출처가 실패해도 다른 출처의 결과는 그대로 응답한다.
 */
@Schema(description = "데이터 출처별 조회 결과")
public record SourceResult<T>(
        @Schema(description = "OK: 조회 성공, NOT_CONFIGURED: 연동 설정 없음, FAILED: 조회 실패")
        Status status,

        @Schema(description = "실패하거나 설정이 없을 때의 안내 문구")
        String message,

        @Schema(description = "조회 결과. OK가 아니면 null")
        T data
) {
    public enum Status { OK, NOT_CONFIGURED, FAILED }

    public static <T> SourceResult<T> ok(T data) {
        return new SourceResult<>(Status.OK, null, data);
    }

    public static <T> SourceResult<T> notConfigured(String message) {
        return new SourceResult<>(Status.NOT_CONFIGURED, message, null);
    }

    public static <T> SourceResult<T> failed(String message) {
        return new SourceResult<>(Status.FAILED, message, null);
    }
}
