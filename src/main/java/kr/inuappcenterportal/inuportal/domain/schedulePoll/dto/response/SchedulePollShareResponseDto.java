package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SchedulePollShareResponseDto(
        @Schema(description = "방마다 보낸 결과")
        List<Result> results
) {

    public record Result(
            @Schema(description = "채팅방 ID", example = "25")
            Long chatRoomId,
            @Schema(description = "카드 메시지 ID", example = "9001")
            Long chatMessageId,
            @Schema(description = "true 새로 보냄 / false 이미 보낸 카드 재사용", example = "true")
            boolean created,
            @Schema(description = "친구 대상으로 보낸 경우 그 친구 회원 ID", example = "205")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Long fromFriendMemberId
    ) {
    }
}
