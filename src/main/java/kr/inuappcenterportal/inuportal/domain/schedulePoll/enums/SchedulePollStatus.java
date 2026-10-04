package kr.inuappcenterportal.inuportal.domain.schedulePoll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 조율 상태는 COLLECTING → RESULT_READY → CLOSED 순으로만 바뀌고 되돌아가지 않는다.
@Getter
@RequiredArgsConstructor
public enum SchedulePollStatus {
    COLLECTING("참여자 0~1명"),
    RESULT_READY("참여자 2명 이상"),
    CLOSED("종료");

    private final String description;
}
