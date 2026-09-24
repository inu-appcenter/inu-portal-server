package kr.inuappcenterportal.inuportal.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public enum SearchTab {
    @Schema(description = "전체 통합 검색 (각 도메인 상위 항목 집계)")
    ALL,
    @Schema(description = "학교 공지사항")
    NOTICE,
    @Schema(description = "학과 공지사항")
    DEPT_NOTICE,
    @Schema(description = "커뮤니티 게시글")
    POST,
    @Schema(description = "학사일정")
    SCHEDULE,
    @Schema(description = "교내 전화번호부")
    DIRECTORY,
    @Schema(description = "개설 강의")
    COURSE,
    @Schema(description = "교내 동아리")
    CLUB
}
