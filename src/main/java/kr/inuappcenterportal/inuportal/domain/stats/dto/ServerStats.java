package kr.inuappcenterportal.inuportal.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "서버 로그·회원 테이블 기반 통계")
public record ServerStats(
        Summary summary,
        List<DailyPoint> daily,
        List<SignupCohort> signupCohorts,
        List<ApiTrend> apiTrends
) {

    @Schema(description = "기간 끝(to) 기준 요약")
    public record Summary(
            @Schema(description = "전체 회원 수") long totalMembers,
            @Schema(description = "to 포함 최근 7일 동안 한 번이라도 접속한 회원 수") long activeMembers7d,
            @Schema(description = "to 포함 최근 30일 동안 한 번이라도 접속한 회원 수") long activeMembers30d,
            @Schema(description = "기간 내 가입자 수") long signups
    ) {
    }

    @Schema(description = "일별 수치")
    public record DailyPoint(
            LocalDate date,
            @Schema(description = "접속한 로그인 회원 수") long loginUsers,
            @Schema(description = "접속한 비로그인 방문자 수(IP 기준, 봇 포함 가능)") long guestUsers,
            @Schema(description = "가입자 수") long signups,
            @Schema(description = "그날 접속 로그가 있는지. false면 로그 요약이 없어 0으로 채운 날") boolean hasLog
    ) {
    }

    @Schema(description = "가입 월별 코호트")
    public record SignupCohort(
            @Schema(description = "가입 월 (yyyy-MM)") String month,
            @Schema(description = "그 달 가입자 수") long signups,
            @Schema(description = "그중 to 기준 최근 30일 안에 접속한 회원 수") long activeMembers30d
    ) {
    }

    @Schema(description = "API 호출 추이. 서버는 하루 상위 20개 API만 요약 저장하므로, 순위 밖으로 밀린 날은 0이다.")
    public record ApiTrend(
            String method,
            String uri,
            @Schema(description = "서버 로그에 처음 나타난 날 (기능 출시일 추정에 사용)") LocalDate firstSeen,
            @Schema(description = "기간 내 호출 수 합계") long total,
            @Schema(description = "기간 내 일별 호출 수 (daily와 같은 날짜 순서)") List<Long> daily
    ) {
    }
}
