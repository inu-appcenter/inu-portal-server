package kr.inuappcenterportal.inuportal.domain.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "통합 검색 응답 DTO")
public class UnifiedSearchResponseDto {

    @Schema(description = "검색어", example = "수강신청")
    private String query;

    @Schema(description = "조회 탭", example = "ALL")
    private SearchTab tab;

    @Schema(description = "검색된 전체 결과 수")
    private long totalCount;

    @Schema(description = "학교 공지사항 섹션")
    private UnifiedSectionDto<NoticeSearchItemDto> notices;

    @Schema(description = "학과 공지사항 섹션")
    private UnifiedSectionDto<DepartmentNoticeSearchItemDto> departmentNotices;

    @Schema(description = "커뮤니티 게시글 섹션")
    private UnifiedSectionDto<PostSearchItemDto> posts;

    @Schema(description = "학사일정 섹션")
    private UnifiedSectionDto<ScheduleSearchItemDto> schedules;

    @Schema(description = "교내 전화번호부 섹션")
    private UnifiedSectionDto<DirectorySearchItemDto> directory;

    @Schema(description = "개설 강의 섹션")
    private UnifiedSectionDto<CourseSearchItemDto> courses;

    @Schema(description = "교내 동아리 섹션")
    private UnifiedSectionDto<ClubSearchItemDto> clubs;
}
