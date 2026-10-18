package kr.inuappcenterportal.inuportal.domain.schedulePoll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 첫 제출 경로. 통계용
@Getter
@RequiredArgsConstructor
public enum JoinSource {
    LINK("참여 링크"),
    CHAT_CARD("채팅 카드");

    private final String description;
}
