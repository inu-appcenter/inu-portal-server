package kr.inuappcenterportal.inuportal.domain.courseReview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssignmentAmount {
    MANY("많음"),
    NORMAL("보통"),
    FEW("적음");

    private final String description;
}
