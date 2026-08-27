package kr.inuappcenterportal.inuportal.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "학과 공지사항 상세 응답 Dto")
@Getter
@NoArgsConstructor
public class DepartmentNoticeDetailResponseDto {

    @Schema(description = "학과 공지사항 데이터베이스 id 값", example = "1")
    private Long id;

    @Schema(description = "학과", example = "COMPUTER_ENGINEERING")
    private Department department;

    @Schema(description = "제목", example = "2026학년도 2학기 수강신청 안내")
    private String title;

    @Schema(description = "작성일", example = "2026.08.06")
    private String createDate;

    @Schema(description = "조회수", example = "94")
    private Long view;

    @Schema(description = "링크 url", example = "https://cse.inu.ac.kr/...")
    private String url;

    @Schema(description = "본문 HTML")
    private String contentHtml;

    @Schema(description = "본문 순수 텍스트")
    private String contentText;

    @Schema(description = "첨부파일 목록")
    private List<AttachmentMeta> attachments;

    @Schema(description = "연결된 일정 존재 여부", example = "true")
    private boolean hasSchedules;

    @Builder
    private DepartmentNoticeDetailResponseDto(
            DepartmentNotice departmentNotice,
            List<AttachmentMeta> attachments,
            boolean hasSchedules
    ) {
        this.id = departmentNotice.getId();
        this.department = departmentNotice.getDepartment();
        this.title = departmentNotice.getTitle();
        this.createDate = departmentNotice.getCreateDate() != null
                ? departmentNotice.getCreateDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                : null;
        this.view = departmentNotice.getView();
        this.url = departmentNotice.getUrl();
        this.contentHtml = departmentNotice.getContentHtml();
        this.contentText = departmentNotice.getContentText();
        this.attachments = attachments;
        this.hasSchedules = hasSchedules;
    }

    public static DepartmentNoticeDetailResponseDto of(
            DepartmentNotice departmentNotice,
            List<AttachmentMeta> attachments,
            boolean hasSchedules
    ) {
        return DepartmentNoticeDetailResponseDto.builder()
                .departmentNotice(departmentNotice)
                .attachments(attachments)
                .hasSchedules(hasSchedules)
                .build();
    }
}
