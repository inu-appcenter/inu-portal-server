package kr.inuappcenterportal.inuportal.domain.course.dto.course.crawlerItem;

public record CourseExcelRow(
        String collegeName,
        String deptName,
        String hyName,
        String isuName,
        String isuFldName,
        String subjectNumber,
        String courseTitle,
        String courseTitleEng,
        String professor,
        String roomName,
        String periodText,
        String timeText,
        String periodType,
        Integer credit,
        String courseCategory,
        String ssupTypeName,
        String cnctrIsuName,
        String gradeEvaluation,
        String englishName
) {
}
