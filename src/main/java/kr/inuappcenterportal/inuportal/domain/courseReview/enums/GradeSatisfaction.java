package kr.inuappcenterportal.inuportal.domain.courseReview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GradeSatisfaction {
    GENEROUS("너그러움"),
    NORMAL("보통"),
    STRICT("깐깐함");

    private final String description;
}
