package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable;

import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;

public record ChatRoomTimeTableResponseDto(
        Long memberId,
        String nickname,
        Visibility visibility,
        TimeTableDetailResponseDto timeTable
) {
}
