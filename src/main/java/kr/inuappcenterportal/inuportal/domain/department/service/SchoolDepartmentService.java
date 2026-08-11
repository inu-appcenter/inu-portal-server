package kr.inuappcenterportal.inuportal.domain.department.service;

import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseOfferingApiItem;
import kr.inuappcenterportal.inuportal.domain.department.dto.SchoolDepartmentResponseDto;
import kr.inuappcenterportal.inuportal.domain.department.model.SchoolDepartment;
import kr.inuappcenterportal.inuportal.domain.department.repository.SchoolDepartmentRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SchoolDepartmentService {
    private final SchoolDepartmentRepository repository;

    @Transactional(readOnly = true)
    public List<SchoolDepartmentResponseDto> getActiveDepartments() {
        return repository.findAllByActiveTrueOrderByNameAscCodeAsc().stream()
                .map(SchoolDepartmentResponseDto::from)
                .toList();
    }

    @Transactional
    public void replaceFromRegularSemester(List<CourseOfferingApiItem> allItems) {
        Optional<CourseOfferingApiItem> latest = allItems.stream()
                .filter(this::isRegularSemester)
                .max(Comparator.comparingInt((CourseOfferingApiItem item) -> Integer.parseInt(item.year()))
                        .thenComparingInt(item -> SemesterTerm.mapToTermCode(item.termCode()) == SemesterTerm.SECOND ? 2 : 1));
        if (latest.isEmpty()) return;

        String year = latest.get().year();
        String termCode = latest.get().termCode();
        Map<String, String> snapshot = new LinkedHashMap<>();
        allItems.stream()
                .filter(this::isRegularSemester)
                .filter(item -> Objects.equals(year, item.year()) && Objects.equals(termCode, item.termCode()))
                .filter(item -> item.deptCode() != null && !item.deptCode().isBlank())
                .filter(item -> item.deptName() != null && !item.deptName().isBlank())
                .forEach(item -> snapshot.put(item.deptCode().trim(), item.deptName().trim()));
        if (snapshot.isEmpty()) return;

        Map<String, SchoolDepartment> existing = new HashMap<>();
        repository.findAll().forEach(department -> existing.put(department.getCode(), department));
        int sourceYear = Integer.parseInt(year);
        String sourceTerm = SemesterTerm.mapToTermCode(termCode).name();

        snapshot.forEach((code, name) -> {
            SchoolDepartment department = existing.get(code);
            if (department == null) repository.save(SchoolDepartment.create(code, name, sourceYear, sourceTerm));
            else department.refresh(name, sourceYear, sourceTerm);
        });
        existing.values().stream()
                .filter(department -> !snapshot.containsKey(department.getCode()))
                .forEach(SchoolDepartment::deactivate);
    }

    private boolean isRegularSemester(CourseOfferingApiItem item) {
        SemesterTerm term = SemesterTerm.mapToTermCode(item.termCode());
        return term == SemesterTerm.FIRST || term == SemesterTerm.SECOND;
    }
}
