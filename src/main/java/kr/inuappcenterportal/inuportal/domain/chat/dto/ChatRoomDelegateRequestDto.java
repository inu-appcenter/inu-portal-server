package kr.inuappcenterportal.inuportal.domain.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomDelegateRequestDto {
    @NotNull(message = "위임할 대상 사용자 ID는 필수입니다.")
    private Long newOwnerChatRoomMemberId;
}
