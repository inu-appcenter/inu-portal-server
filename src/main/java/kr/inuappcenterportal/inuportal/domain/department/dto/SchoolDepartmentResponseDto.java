package kr.inuappcenterportal.inuportal.domain.department.dto;

import kr.inuappcenterportal.inuportal.domain.department.model.SchoolDepartment;

/**
 * departmentEnum: 학사 학과명에 대응하는 Department enum 코드. 매핑을 못 찾으면 null(issue #400) —
 * 이 학과의 공지 서비스가 아직 없다는 뜻은 아니다(그건 noticeAvailable로 구분한다).
 */
public record SchoolDepartmentResponseDto(String code, String name, String departmentEnum, boolean noticeAvailable) {
    public static SchoolDepartmentResponseDto from(SchoolDepartment department) {
        return new SchoolDepartmentResponseDto(
                department.getCode(),
                department.getName(),
                department.getResolvedNoticeDepartment() == null
                        ? null
                        : department.getResolvedNoticeDepartment().name(),
                department.getResolvedNoticeDepartment() != null
                        && department.getResolvedNoticeDepartment().isServiceAvailable()
        );
    }
}
