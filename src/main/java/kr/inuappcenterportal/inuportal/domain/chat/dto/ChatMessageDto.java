package kr.inuappcenterportal.inuportal.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageDto {
    private String roomId;
    private String sender;
    private String message;
}
