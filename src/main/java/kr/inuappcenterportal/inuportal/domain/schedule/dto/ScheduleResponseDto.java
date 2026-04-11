package kr.inuappcenterportal.inuportal.domain.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Schema(description = "학사일정 응답 Dto")
@Getter
@NoArgsConstructor
public class ScheduleResponseDto {
    @Schema(description = "내용", example = "수강신청")
    private String title;

    @Schema(description = "시작일", example = "yyyy-mm-dd")
    private String start;

    @Schema(description = "종료일", example = "yyyy-mm-dd")
    private String end;

    @Schema(description = "AI 생성 일정 여부", example = "false")
    private boolean aiGenerated;

    @Schema(description = "학과명", example = "컴퓨터공학부")
    private String department;

    @Builder
    private ScheduleResponseDto(String start, String end, String title, boolean aiGenerated, String department) {
        this.start = start;
        this.end = end;
        this.title = title;
        this.aiGenerated = aiGenerated;
        this.department = department;
    }

    public static ScheduleResponseDto of(Schedule schedule) {
        return ScheduleResponseDto.builder()
                .start(schedule.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .end(schedule.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .title(schedule.getContent())
                .aiGenerated(schedule.isAiGenerated())
                .department(schedule.getDepartment() == null ? null : schedule.getDepartment().getDepartmentName())
                .build();
    }
}
