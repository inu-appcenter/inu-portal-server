package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ShareKind;

import java.util.List;

public record SchedulePollShareRequestDto(
        @Schema(description = "INVITE 참여 요청 카드 / RESULT 결과 카드", example = "RESULT")
        @NotNull
        ShareKind kind,

        @Schema(description = "RESULT일 때 필수. 최신 버전이어야 함(끝난 조율이면 최종 버전)", example = "3")
        Integer resultVersion,

        @Schema(description = "RESULT일 때 필수. 카드에 표시할 후보 1~3", example = "1")
        Integer candidateRank,

        @Schema(description = "내가 참여 중인 채팅방 ID", example = "[25, 31]")
        List<Long> chatRoomIds,

        @Schema(description = "친구 회원 ID. 1:1 방으로 바꿔 보낸다(없으면 생성)", example = "[205]")
        List<Long> friendMemberIds
) {
}
