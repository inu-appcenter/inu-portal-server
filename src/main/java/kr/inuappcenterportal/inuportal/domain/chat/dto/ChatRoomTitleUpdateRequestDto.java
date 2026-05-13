package kr.inuappcenterportal.inuportal.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomTitleUpdateRequestDto {
    @NotBlank(message = "채팅방 이름은 필수입니다.")
    private String title;
}
