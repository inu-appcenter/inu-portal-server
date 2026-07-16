package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;

public record TimeTableVisibilityUpdateRequestDto(
        @NotNull
        Visibility visibility
) {
}
