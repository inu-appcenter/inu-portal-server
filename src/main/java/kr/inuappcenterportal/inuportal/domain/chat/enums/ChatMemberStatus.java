package kr.inuappcenterportal.inuportal.domain.chat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatMemberStatus {
    JOINED("참여 중"),
    LEFT("나감"),
    KICKED("강퇴됨");

    private final String description;
}
