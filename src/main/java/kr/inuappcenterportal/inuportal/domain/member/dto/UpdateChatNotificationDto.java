package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "채팅 알림 설정 업데이트 요청 DTO")
@Getter
@NoArgsConstructor
public class UpdateChatNotificationDto {
    @Schema(description = "채팅 알림 수신 여부", example = "true")
    private boolean chatNotification;
}
