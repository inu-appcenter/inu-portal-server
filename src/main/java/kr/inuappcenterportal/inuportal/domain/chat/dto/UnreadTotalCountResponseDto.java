package kr.inuappcenterportal.inuportal.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UnreadTotalCountResponseDto {
    private long totalUnreadCount;
}
