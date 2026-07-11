package kr.inuappcenterportal.inuportal.domain.course.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Method {
    ONLINE("온라인"),
    OFFLINE("오프라인"),
    BLENDED("온/오프 혼합"),
    UNKNOWN("미정");

    private final String description;
}
