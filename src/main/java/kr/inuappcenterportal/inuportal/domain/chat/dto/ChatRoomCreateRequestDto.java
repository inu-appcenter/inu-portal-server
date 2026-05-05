package kr.inuappcenterportal.inuportal.domain.chat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomCreateRequestDto {
    @NotBlank(message = "채팅방 제목은 필수입니다.")
    private String title;

    @Min(value = 2, message = "최대 참여 인원은 최소 2명 이상이어야 합니다.")
    private int maxCapacity;

    @NotNull(message = "익명 채팅방 여부는 필수입니다.")
    private Boolean isAnonymous;
}
