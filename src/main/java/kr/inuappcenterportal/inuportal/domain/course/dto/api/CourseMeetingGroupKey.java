package kr.inuappcenterportal.inuportal.domain.course.dto.api;

// 개설 강의와 시간 정보 매핑을 위한 내부 객체
public record CourseMeetingGroupKey(
        String year,
        String termCode,
        String haksuCode
) {
}
