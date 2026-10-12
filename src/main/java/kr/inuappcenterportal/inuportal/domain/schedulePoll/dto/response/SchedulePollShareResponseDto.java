package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
            // 메시지 ID는 2^53을 넘어서 숫자로 내리면 브라우저에서 끝자리가 바뀐다. 채팅 조회 응답처럼 문자열로 내린다.
            @Schema(description = "카드 메시지 ID", type = "string", example = "897327790404474987")
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            Long chatMessageId,
            @Schema(description = "true 새로 보냄 / false 이미 보낸 카드 재사용", example = "true")
            boolean created,
            @Schema(description = "친구 대상으로 보낸 경우 그 친구 회원 ID", example = "205")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Long fromFriendMemberId
    ) {
    }
}
