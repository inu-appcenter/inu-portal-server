package kr.inuappcenterportal.inuportal.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PersonalChatRoomRequestDto {
    private List<Long> targetMemberIds;
    private boolean adminMode;
    private String title;
}
