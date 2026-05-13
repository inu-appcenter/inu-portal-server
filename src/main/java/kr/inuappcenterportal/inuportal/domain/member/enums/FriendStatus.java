package kr.inuappcenterportal.inuportal.domain.member.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FriendStatus {
    PENDING("대기"),
    ACCEPTED("수락");

    private final String description;
}
