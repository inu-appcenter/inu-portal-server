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

    @Schema(description = "그날 수치를 어디서 읽었는지")
    public enum LogStatus {
        @Schema(description = "자정에 만든 하루 요약") SUMMARY,
        @Schema(description = "아직 요약 전이라 원본 로그에서 읽음 (오늘, 자정 직후의 어제). 값이 계속 바뀔 수 있음") RAW,
        @Schema(description = "요약도 원본 로그도 없음. 수치는 0으로 채움") MISSING
    }

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
            @Schema(description = "접속한 회원 수 (현재 존재하는 회원 ID만)") long loginUsers,
            @Schema(description = "접속한 비로그인 방문자 수 (IP 기준, 봇 포함 가능)") long guestUsers,
            @Schema(description = "가입자 수") long signups,
            LogStatus logStatus
    ) {
    }

    @Schema(description = "가입 월별 코호트")
    public record SignupCohort(
            @Schema(description = "가입 월 (yyyy-MM)") String month,
            @Schema(description = "그 달 가입자 수 (조회 기간 안에 든 날짜만)") long signups,
            @Schema(description = "그중 to 기준 최근 30일 안에 접속한 회원 수") long activeMembers30d,
            @Schema(description = "조회 기간이 그 달의 일부만 포함하면 true") boolean partial
    ) {
    }

    @Schema(description = "API 호출 추이. 서버는 하루 상위 20개 API만 저장하므로 순위 밖으로 밀린 날은 0이다. " +
            "URI는 경로 변수가 채워진 실제 주소라 /api/posts/1, /api/posts/2처럼 따로 집계된다.")
    public record ApiTrend(
            String method,
            String uri,
            @Schema(description = "로그 요약이 시작된 뒤 이 API가 처음 하루 상위 20위에 든 날. 출시일과 다를 수 있음") LocalDate firstSeen,
            @Schema(description = "기간 내 호출 수 합계") long total,
            @Schema(description = "기간 내 일별 호출 수 (daily와 같은 날짜 순서). 0은 상위 20위 밖이거나 로그가 없는 날") List<Long> daily
    ) {
    }
}
