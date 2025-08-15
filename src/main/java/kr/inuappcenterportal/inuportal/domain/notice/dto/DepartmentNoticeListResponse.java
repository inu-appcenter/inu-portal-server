package kr.inuappcenterportal.inuportal.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;

@Schema(description = "각 학과 별 공지사항 리스트 응답")
public record DepartmentNoticeListResponse(

        @Schema(description = "공지사항 데이터베이스 id 값", example = "1")
        Long id,

        @Schema(description = "제목", example = "2025학년도 2학기 전공심화트랙 이수 신청 안내")
        String title,

        @Schema(description = "학과", example = "컴퓨터공학과")
        Department department,

        @Schema(description = "작성일", example = "2025.08.06")
        String createDate,

        @Schema(description = "조회수", example = "94")
        Long view,

        @Schema(description = "링크 url", example = "https://example.com")
        String url

) {
    public static DepartmentNoticeListResponse of (DepartmentNotice departmentNotice) {
        return new DepartmentNoticeListResponse(
                departmentNotice.getId(),
                departmentNotice.getTitle(),
                departmentNotice.getDepartment(),
                departmentNotice.getCreateDate(),
                departmentNotice.getView(),
                departmentNotice.getUrl()
        );
    }
}
