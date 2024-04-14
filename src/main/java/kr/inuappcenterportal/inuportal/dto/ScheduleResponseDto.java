package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Schedule;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Schema(description = "학사일정 응답 Dto")
@Getter
@NoArgsConstructor
public class ScheduleResponseDto {
    @Schema(description = "시작날짜",example = "yyyy.mm.dd")
    private String startDate;
    @Schema(description = "종료날짜",example = "yyyy.mm.dd")
    private String endDate;
    @Schema(description = "내용",example = "수강신청")
    private String content;

    @Builder
    private ScheduleResponseDto (String startDate, String endDate, String content){
        this.startDate = startDate;
        this.endDate = endDate;
        this.content = content;
    }
    public static ScheduleResponseDto of(Schedule schedule){
        return ScheduleResponseDto.builder()
                .startDate(schedule.getStartDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .endDate(schedule.getEndDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .content(schedule.getContent())
                .build();
    }
}
