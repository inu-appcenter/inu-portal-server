package kr.inuappcenterportal.inuportal.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "관리자 통계 대시보드 응답")
public record AdminStatsResponse(
        LocalDate from,
        LocalDate to,
        LocalDateTime generatedAt,

        @Schema(description = "서버 로그·회원 테이블 기반 통계")
        SourceResult<ServerStats> server,

        @Schema(description = "Mixpanel 기반 통계 (앱/브라우저 비교, 재방문율 등). 연동 전에는 NOT_CONFIGURED")
        SourceResult<Object> mixpanel
) {
}
