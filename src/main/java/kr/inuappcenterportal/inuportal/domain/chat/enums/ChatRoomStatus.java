package kr.inuappcenterportal.inuportal.domain.chat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatRoomStatus {
    ACTIVE("활성"),
    CLOSED("폐쇄");

    private final String description;
}
