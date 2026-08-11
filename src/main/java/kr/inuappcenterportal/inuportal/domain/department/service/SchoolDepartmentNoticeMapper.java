package kr.inuappcenterportal.inuportal.domain.department.service;

import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;

import java.util.Map;
import java.util.Optional;

public final class SchoolDepartmentNoticeMapper {
    private static final Map<String, Department> NAME_ALIASES = Map.ofEntries(
            Map.entry("무역학부", Department.TRADE),
            Map.entry("Global Trade & Service 학부", Department.TRADE),
            Map.entry("Global Trade & Service학부", Department.TRADE),
            Map.entry("테크노경영학과", Department.TECHNO_MANAGEMENT),
            Map.entry("건설환경공학전공", Department.CIVIL_ENVIRONMENT_ENGINEERING),
            Map.entry("도시건축학전공", Department.URBAN_ARCHITECTURE),
            Map.entry("환경공학전공", Department.ENVIRONMENT_ENGINEERING),
            Map.entry("바이오-로봇시스템공학과", Department.BIO_ROBOTICS_ENGINEERING),
            Map.entry("나노바이오공학전공", Department.BIOENGINEERING_NANO),
            Map.entry("생명공학부", Department.BIOENGINEERING),
            Map.entry("생명공학전공", Department.BIOENGINEERING),
            Map.entry("분자의생명전공", Department.LIFE_SCIENCE_MOLECULAR),
            Map.entry("생명과학부", Department.LIFE_SCIENCE),
            Map.entry("생명과학전공", Department.LIFE_SCIENCE),
            Map.entry("전자공학부", Department.ELECTRONICS_ENGINEERING),
            Map.entry("전자공학전공", Department.ELECTRONICS_ENGINEERING)
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
