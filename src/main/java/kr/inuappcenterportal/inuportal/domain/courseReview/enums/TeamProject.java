package kr.inuappcenterportal.inuportal.domain.courseReview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeamProject {
    MANY("많음"),
    NORMAL("보통"),
    NONE("없음");

    private final String description;
}
