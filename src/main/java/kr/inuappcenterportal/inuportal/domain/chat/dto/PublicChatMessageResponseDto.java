package kr.inuappcenterportal.inuportal.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor // 역직렬화를 위한 기본 생성자
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicChatMessageResponseDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long messageId;

    private String senderNickname;
    private String content;
    private LocalDateTime createDate;

    private Integer imageCount;
    private int unreadCount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicChatMessageResponseDto that)) return false;
        return messageId != null && messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        return messageId != null ? messageId.hashCode() : 0;
    }

    public static PublicChatMessageResponseDto from(ChatMessage chatMessage) {
        return PublicChatMessageResponseDto.builder()
                .messageId(chatMessage.getId())
                .senderNickname(chatMessage.getSenderNickname())
                .content(chatMessage.getContent())
                .createDate(chatMessage.getCreateDate())
                .imageCount(chatMessage.getImageCount())
                .build();
    }
}