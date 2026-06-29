package kr.inuappcenterportal.inuportal.domain.semester.enums;

import lombok.Getter;

@Getter
public enum SemesterStatus {
    UPCOMING, // 아직 열리지 않음
    OPEN,     // 시간표 생성/수정 가능
    CLOSED    // 학기 종료, 기존 시간표 조회만 가능
}
