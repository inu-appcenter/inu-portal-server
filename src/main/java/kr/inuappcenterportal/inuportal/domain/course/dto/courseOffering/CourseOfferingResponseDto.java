package kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;

import java.util.List;

public record CourseOfferingResponseDto(
        Long id,
        String syllabus,
        String subjectNumber,
        String professor,

        Long courseId,
        String courseCode,
        String courseTitle,
        String courseTitleEng,

        Long semesterId,
        Integer year,
        SemesterTerm term,
        String termName,

        String cnctrIsuCode,
        String cnctrIsuName,

        String deptCode,
        String deptName,

        String collegeCode,
        String collegeName,

        String isuFldCode,
        String isuFldName,

        String isuCode,
        String isuName,

        String ssupTypeCode,
        String ssupTypeName,

        String hyCode,
        String hyName,

        String englishCode,
        String englishName,
        String englishYn,

        String gradeEvaluationCode,
        String gradeEvaluationName,

        Integer credit,

        @Schema(description = "공식 정원. 편람/정원 엑셀에서 파싱된 값이며, 원천 데이터가 없으면 null입니다.")
        Integer capacity,

        @Schema(description = "실제 수강 인원. 현재 원천 데이터 미연동 시 null입니다.")
        Integer enrolledCount,

        @Schema(description = "포털 앱 내 시간표에 해당 강의를 담은 회원 수입니다.")
        Long savedCount,

        String hussCourseYn,
        String note,
        List<CourseMeetingResponseDto> meetings
) {

    // 교수명 노출 제한을 신경 쓰지 않는 내부 코드나 테스트, 혹은 기존 호출부 호환용
    public static CourseOfferingResponseDto from(CourseOffering courseOffering, List<CourseMeetingResponseDto> meetings) {
        return from(courseOffering, meetings, true, 0L);
    }

    public static CourseOfferingResponseDto from(
            CourseOffering courseOffering,
            List<CourseMeetingResponseDto> meetings,
            boolean exposeProfessor
    ) {
        return from(courseOffering, meetings, exposeProfessor, 0L);
    }

    // 교수명 노출 여부 및 담은 인원수를 제어하는 변환
    public static CourseOfferingResponseDto from(
            CourseOffering courseOffering,
            List<CourseMeetingResponseDto> meetings,
            boolean exposeProfessor,
            Long savedCount
    ) {
        return new CourseOfferingResponseDto(
                courseOffering.getId(),
                courseOffering.getSyllabus(),
                courseOffering.getSubjectNumber(),
                exposeProfessor ? courseOffering.getProfessor() : null,
                courseOffering.getCourse().getId(),
                courseOffering.getCourse().getCourseCode(),
                courseOffering.getCourse().getTitle(),
                courseOffering.getCourse().getEnglishTitle(),
                courseOffering.getSemester().getId(),
                courseOffering.getSemester().getYear(),
                courseOffering.getSemester().getTerm(),
                courseOffering.getSemester().getTerm().getDisplayName(),

                valueOrFallback(courseOffering.getCnctrIsuCode(), courseOffering.getCnctrIsuName().name()),
                valueOrFallback(courseOffering.getCnctrIsuNameRaw(), courseOffering.getCnctrIsuName().getDescription()),
                valueOrFallback(courseOffering.getDeptCode(), courseOffering.getDeptName().name()),
                valueOrFallback(courseOffering.getDeptNameRaw(), courseOffering.getDeptName().getDescription()),
                valueOrFallback(courseOffering.getCollegeCode(), courseOffering.getCollegeName().name()),
                valueOrFallback(courseOffering.getCollegeNameRaw(), courseOffering.getCollegeName().getDescription()),
                valueOrFallback(courseOffering.getIsuFldCode(), courseOffering.getIsuFldName().name()),
                valueOrFallback(courseOffering.getIsuFldNameRaw(), courseOffering.getIsuFldName().getDescription()),
                valueOrFallback(courseOffering.getIsuCode(), courseOffering.getIsuName().name()),
                valueOrFallback(courseOffering.getIsuNameRaw(), courseOffering.getIsuName().getDescription()),
                valueOrFallback(courseOffering.getSsupTypeCode(), courseOffering.getSsupTypeName().name()),
                valueOrFallback(courseOffering.getSsupTypeNameRaw(), courseOffering.getSsupTypeName().getDescription()),
                valueOrFallback(courseOffering.getHyCode(), courseOffering.getHyName().name()),
                valueOrFallback(courseOffering.getHyNameRaw(), courseOffering.getHyName().getDescription()),
                valueOrFallback(courseOffering.getEnglishCode(), courseOffering.getEnglishName().name()),
                valueOrFallback(courseOffering.getEnglishNameRaw(), courseOffering.getEnglishName().getDescription()),
                courseOffering.getEnglishYn(),
                courseOffering.getGradeEvaluation() == null ? null : courseOffering.getGradeEvaluation().name(),
                valueOrFallback(courseOffering.getGradeEvaluationRaw(), courseOffering.getGradeEvaluation() == null ? null : courseOffering.getGradeEvaluation().getDescription()),

                courseOffering.getCredit(),
                courseOffering.getCapacity(),
                courseOffering.getEnrolledCount(),
                savedCount,
                courseOffering.getHussCourseYn(),
                courseOffering.getNote(),
                meetings
        );
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
