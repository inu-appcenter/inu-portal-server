package kr.inuappcenterportal.inuportal.domain.department.dto;

import kr.inuappcenterportal.inuportal.domain.department.model.SchoolDepartment;

public record SchoolDepartmentResponseDto(String code, String name, boolean noticeAvailable) {
    public static SchoolDepartmentResponseDto from(SchoolDepartment department) {
        return new SchoolDepartmentResponseDto(
                department.getCode(),
                department.getName(),
                department.getResolvedNoticeDepartment() != null
                        && department.getResolvedNoticeDepartment().isServiceAvailable()
        );
    }
}
