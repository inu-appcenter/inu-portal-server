package kr.inuappcenterportal.inuportal.domain.firebase.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AdminNotificationRequest(

        @Schema(description = "전송할 회원 Id", example = "[1, 2, 3]")
        List<Long> memberIds,

        @Schema(description = "제목", example = "설문조사 이벤트")
        @NotBlank(message = "제목이 비어있습니다.")
        String title,

        @Schema(description = "내용", example = "설문조사를 참여해주세요! 보상을 드려요!")
        @NotBlank(message = "내용이 비어있습니다.")
        String content

) {
}
