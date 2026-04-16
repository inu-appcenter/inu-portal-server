package kr.inuappcenterportal.inuportal.domain.schedule.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "학사일정 목록 응답")
@Getter
@NoArgsConstructor
public class ScheduleListResponseDoc {

    @ArraySchema(schema = @Schema(implementation = ScheduleResponseDto.class))
    private List<ScheduleResponseDto> data;

    @Schema(description = "응답 메시지", example = "학사일정 가져오기 성공")
    private String msg;
}
