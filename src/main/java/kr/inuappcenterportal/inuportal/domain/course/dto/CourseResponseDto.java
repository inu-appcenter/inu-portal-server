package kr.inuappcenterportal.inuportal.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.course.enums.CompletionDivision;
import kr.inuappcenterportal.inuportal.domain.course.enums.TargetGrade;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
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

        @Schema(description = "대상학년코드", example = "THIRD")
        TargetGrade targetGradeCode,
        @Schema(description = "대상학년", example = "3학년")
        String targetGradeName,

        @Schema(description = "이수구분코드", example = "ESSENTIAL_MAJOR")
        CompletionDivision completionDivisionCode,
        @Schema(description = "이수구분", example = "전공필수")
        String completionDivisionName,

        @Schema(description = "학점", example = "3")
        String credit,

        @Schema(description = "강의개요", example = "운영체제의 Process 구현, 동기화, 기억장치 운영, 자원분배, 시스템 보안 등에 대하여 연구하며, 대형컴퓨터의 사례연구와 실제 설계의 구성 능력을 배양한다.")
        String content
) {
    public static CourseResponseDto from(Course course) {
        return new CourseResponseDto(
                course.getId(),
                course.getTitle(),
                course.getDepartment(),
                course.getDepartment().getDocumentName(),
                course.getTargetGrade(),
                course.getTargetGrade().getDisplayName(),
                course.getCompletionDivision(),
                course.getCompletionDivision().getDescription(),
                course.getCredit(),
                course.getContent()
        );
    }
}
