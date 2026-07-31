package kr.inuappcenterportal.inuportal.domain.course.dto.course.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.course.enums.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetTerm;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.department.enums.College;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;

public record CourseResponseDto(

        @Schema(description = "강의 id", example = "1")
        Long id,

        @Schema(description = "강의명", example = "운영체제")
        String title,

        @Schema(description = "학과코드", example = "COMPUTER_ENGINEERING")
        Department departmentCode,
        @Schema(description = "학과명", example = "컴퓨터공학부")
        String departmentName,

        @Schema(description = "단과대코드", example = "COLLEGE_OF_INFORMATION_TECHNOLOGY")
        College collegeCode,
        @Schema(description = "단과대명", example = "정보기술대학")
        String collegeName,

        @Schema(description = "대상학년코드", example = "THIRD")
        TargetGrade targetGradeCode,
        @Schema(description = "대상학년", example = "3학년")
        String targetGradeName,
        @Schema(description = "대상학기코드", example = "FIRST")
        TargetTerm targetTermCode,
        @Schema(description = "대상학기", example = "1학기")
        String targetTermName,

        @Schema(description = "이수구분코드", example = "ESSENTIAL_MAJOR")
        CompletionDivision completionDivisionCode,
        @Schema(description = "이수구분", example = "전공필수")
        String completionDivisionName,

        @Schema(description = "학점", example = "3")
        Integer credit,

        @Schema(description = "강의개요", example = "운영체제의 Process 구현, 동기화, 기억장치 운영, 자원분배, 시스템 보안 등에 대하여 연구하며, 대형컴퓨터의 사례연구와 실제 설계의 구성 능력을 배양한다.")
        String content,

        @Schema(description = "강의개설", example = "true")
        boolean active
) {
    public static CourseResponseDto from(Course course) {
        return new CourseResponseDto(
                course.getId(),
                course.getTitle(),
                course.getDepartment(),
                course.getDepartment().getDepartmentName(),
                course.getCollege(),
                course.getCollege().getCollegeName(),

                course.getTargetGrade(),
                course.getTargetGrade() == null ? "대상 학년 정보 없음" : course.getTargetGrade().getDisplayName(),

                course.getTargetTerm(),
                course.getTargetTerm() == null ? "대상 학기 정보 없음" : course.getTargetTerm().getDisplayName(),

                course.getCompletionDivision(),
                course.getCompletionDivision() == null ? "이수 구분 정보 없음" : course.getCompletionDivision().getDescription(),

                course.getCredit(),
                course.getContent() == null ? "교과목개요가 존재하지 않습니다" : course.getContent(),
                course.isActive()
        );
    }
}
