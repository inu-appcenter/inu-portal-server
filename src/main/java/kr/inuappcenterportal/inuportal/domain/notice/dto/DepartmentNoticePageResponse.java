package kr.inuappcenterportal.inuportal.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "학과 공지사항 페이지 응답")
public record DepartmentNoticePageResponse(

        @Schema(description = "총 페이지 수", example = "10")
        long pages,

        @Schema(description = "총 게시글 수", example = "80")
        long total,

        @Schema(description = "게시글 리스트")
        List<DepartmentNoticeListResponse> contents,

        @Schema(description = "서비스 가능 여부", example = "true")
        boolean isServiceAvailable,

        @Schema(description = "게시글 내용 조회 가능 여부", example = "true")
        boolean isContentAvailable
) {
    public static DepartmentNoticePageResponse of(long pages, long total, List<DepartmentNoticeListResponse> contents, boolean isServiceAvailable, boolean isContentAvailable) {
        return new DepartmentNoticePageResponse(pages, total, contents, isServiceAvailable, isContentAvailable);
    }
}
