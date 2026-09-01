package kr.inuappcenterportal.inuportal.domain.course.dto;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;

/**
 * 과거 데이터 엑셀 파일을 위한 일회용 record
 */
public record LegacyCourseRow(
        Integer year,
        SemesterTerm term,
        String collegeRaw,
        String departmentRaw,
        String completionDivisionRaw,
        String gradeRaw,
        String subjectNumber,
        String title,
        String classMethodRaw,
        String professor,
        String timetable,
        Integer credit,
        String isuFldRaw,
        String courseCategoryRaw,
        String ssupTypeRaw,
        String cnctrIsuRaw,
        String englishYn
) {
}
