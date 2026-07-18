package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TimeTableCustomItemRequestDto(
        String memo,
        @NotBlank String title,
        @NotEmpty List<@Valid TimeTableCustomMeetingRequestDto> meetings
) {
}
