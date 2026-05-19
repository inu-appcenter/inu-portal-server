package kr.inuappcenterportal.inuportal.domain.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class ChatRoomInviteRequestDto {
    @NotEmpty(message = "초대할 친구 ID 목록은 필수입니다.")
    private List<Long> targetFriendIds;
}
