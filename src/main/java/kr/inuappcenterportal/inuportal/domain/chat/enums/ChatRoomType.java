package kr.inuappcenterportal.inuportal.domain.chat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatRoomType {
    PERSONAL("개인/단톡"),
    OPEN("오픈채팅");

    private final String description;
}
