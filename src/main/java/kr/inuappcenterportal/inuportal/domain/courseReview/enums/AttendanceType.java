package kr.inuappcenterportal.inuportal.domain.courseReview.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceType {
    ELECTRONIC("전자출결"),
    CALL("직접호명"),
    MIXED("혼합"),
    NONE("반영안함");

    private final String description;
}
