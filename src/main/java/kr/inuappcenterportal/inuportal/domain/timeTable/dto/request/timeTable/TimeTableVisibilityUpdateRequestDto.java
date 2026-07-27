package kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;

public record TimeTableVisibilityUpdateRequestDto(
        @Schema(
                description = "공개범위. PUBLIC은 친구에게 전체 공개, PROTECTED는 친구에게 요일/시간만 공개, PRIVATE은 비공개입니다.",
                allowableValues = {"PUBLIC", "PROTECTED", "PRIVATE"},
                example = "PUBLIC"
        )
        @NotNull
        Visibility visibility
) {
}
