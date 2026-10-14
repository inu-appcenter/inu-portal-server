package kr.inuappcenterportal.inuportal.domain.schedulePoll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 내 조율 목록의 구분. 조회 응답의 status 표시 규칙과 같아서, 마감 시각이 지난 조율은 CLOSED 쪽에 나온다.
@Getter
@RequiredArgsConstructor
public enum MyPollFilter {

    ACTIVE("진행 중"),
    CLOSED("끝났거나 마감이 지남");

    private final String description;
}
