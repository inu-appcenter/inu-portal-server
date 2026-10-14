package kr.inuappcenterportal.inuportal.domain.schedulePoll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 응답의 myStatus. 제출하지 않았으면 null로 내려 준다.
@Getter
@RequiredArgsConstructor
public enum ParticipantStatus {
    SUBMITTED("제출함");

    private final String description;
}
