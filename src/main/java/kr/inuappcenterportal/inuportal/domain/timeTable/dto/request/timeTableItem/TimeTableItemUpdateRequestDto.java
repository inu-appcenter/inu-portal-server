package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TimeTableItemUpdateRequestDto(
        @Schema(description = "커스텀 일정 제목", example = "알고리즘 스터디")
        @NotBlank String title,
        @Schema(description = "커스텀 일정 시간 목록")
        @NotEmpty List<@Valid TimeTableCustomMeetingRequestDto> meetings
) {
}
