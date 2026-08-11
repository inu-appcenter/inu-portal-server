package kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;

import java.util.List;

public record CourseOfferingOptionsResponseDto(
        SemesterOption semester,
        List<CodeNameOption> departments,
        List<CompletionOption> completionCategories,
        List<CodeNameOption> connectedMajors
) {
    public record SemesterOption(Long id, Integer year, SemesterTerm term, String termName) {}
    public record CodeNameOption(String code, String name) {}
    public record CompletionOption(String code, String name, List<CodeNameOption> fields) {}
}
