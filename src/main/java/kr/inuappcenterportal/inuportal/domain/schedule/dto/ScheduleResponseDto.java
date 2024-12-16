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
    @Schema(description = "내용",example = "수강신청")
    private String title;
    @Schema(description = "시작날짜",example = "yyyy-mm-dd")
    private String start;
    @Schema(description = "종료날짜",example = "yyyy-mm-dd")
    private String end;


    @Builder
    private ScheduleResponseDto (String start, String end, String title){
        this.start = start;
        this.end = end;
        this.title = title;
    }
    public static ScheduleResponseDto of(Schedule schedule){
        return ScheduleResponseDto.builder()
                .start(schedule.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .end(schedule.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .title(schedule.getContent())
                .build();
    }
}
