package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request;

import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;

public record TimeTableVisibilityUpdateRequestDto(
        Visibility visibility
) {
}
