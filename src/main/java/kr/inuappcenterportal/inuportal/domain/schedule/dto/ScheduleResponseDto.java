package kr.inuappcenterportal.inuportal.domain.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Schema(description = "학사일정 응답 Dto")
@Getter
@NoArgsConstructor
public class ScheduleResponseDto {

    @Schema(description = "일정 id", example = "101")
    private Long id;

    @Schema(description = "제목", example = "수강신청")
    private String title;

    @Schema(description = "설명", example = "2026년 3월 9일 오전 9시부터 선착순 마감으로 신청을 받습니다.")
    private String description;

    @Schema(description = "시작일", example = "yyyy-mm-dd")
    private String start;

    @Schema(description = "종료일", example = "yyyy-mm-dd")
    private String end;

    @Schema(description = "AI 생성 일정 여부", example = "false")
    private boolean aiGenerated;

    @Schema(description = "학과명", example = "컴퓨터공학부")
    private String department;

    @Schema(description = "원본 학과 공지 id", example = "123")
    private Long sourceNoticeId;

    @Schema(description = "원본 학과 공지 제목", example = "2026학년도 비교과 프로그램 신청 안내")
    private String sourceNoticeTitle;

    @Schema(description = "원본 학과 공지 url", example = "https://cse.inu.ac.kr/isis/3519/subview.do?enc=...")
    private String url;

    @Builder
    private ScheduleResponseDto(
            Long id,
            String start,
            String end,
            String title,
            String description,
            boolean aiGenerated,
            String department,
            Long sourceNoticeId,
            String sourceNoticeTitle,
            String url
    ) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.title = title;
        this.description = description;
        this.aiGenerated = aiGenerated;
        this.department = department;
        this.sourceNoticeId = sourceNoticeId;
        this.sourceNoticeTitle = sourceNoticeTitle;
        this.url = url;
    }

    public static ScheduleResponseDto of(Schedule schedule) {
        return of(schedule, null);
    }

    public static ScheduleResponseDto of(Schedule schedule, DepartmentNotice sourceNotice) {
        return ScheduleResponseDto.builder()
                .id(schedule.getId())
                .start(schedule.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .end(schedule.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .title(schedule.getContent())
                .description(schedule.getDescription())
                .aiGenerated(schedule.getAiGenerated() != null && schedule.getAiGenerated())
                .department(schedule.getDepartment() == null ? null : schedule.getDepartment().getDepartmentName())
                .sourceNoticeId(schedule.getSourceNoticeId())
                .sourceNoticeTitle(sourceNotice == null ? null : sourceNotice.getTitle())
                .url(sourceNotice == null ? null : sourceNotice.getUrl())
                .build();
    }
}
