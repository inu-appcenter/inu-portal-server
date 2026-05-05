package kr.inuappcenterportal.inuportal.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequestDto {
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long roomId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    private String content;

    @NotNull(message = "익명 여부는 필수입니다.")
    private Boolean isAnonymous;
}
