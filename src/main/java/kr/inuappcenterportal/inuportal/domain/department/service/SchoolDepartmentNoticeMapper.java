package kr.inuappcenterportal.inuportal.domain.department.service;

import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;

import java.util.Map;
import java.util.Optional;

public final class SchoolDepartmentNoticeMapper {
    private static final Map<String, Department> NAME_ALIASES = Map.of(
            "무역학부", Department.TRADE,
            "Global Trade & Service 학부", Department.TRADE,
            "테크노경영학과", Department.TECHNO_MANAGEMENT
    );

    private SchoolDepartmentNoticeMapper() {}

    public static Optional<Department> find(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) return Optional.empty();
        String normalized = departmentName.trim();
        Department alias = NAME_ALIASES.get(normalized);
        if (alias != null) return Optional.of(alias);
        return java.util.Arrays.stream(Department.values())
                .filter(department -> department.getDepartmentName().equals(normalized))
                .findFirst();
    }
}
