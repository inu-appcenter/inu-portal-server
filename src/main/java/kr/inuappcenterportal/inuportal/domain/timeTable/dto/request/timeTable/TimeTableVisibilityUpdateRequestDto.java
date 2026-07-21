package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable;

import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;

public record TimeTableVisibilityUpdateRequestDto(
        @NotNull
        Visibility visibility
) {
}
