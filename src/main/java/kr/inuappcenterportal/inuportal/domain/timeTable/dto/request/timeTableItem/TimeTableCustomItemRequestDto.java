package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TimeTableCustomItemRequestDto(
        @Schema(description = "커스텀 일정 제목", example = "알고리즘 스터디")
        @NotBlank String title,
        @Schema(description = "시간표 요소 메모", example = "스터디룸 예약")
        String memo,
        @Schema(description = "커스텀 일정 시간 목록")
        @NotEmpty List<@Valid TimeTableCustomMeetingRequestDto> meetings
) {
}
