package kr.inuappcenterportal.inuportal.domain.courseReview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.inuappcenterportal.inuportal.domain.courseReview.enums.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "강의평 수정 요청 DTO")
public class CourseReviewUpdateDto {

    @NotNull(message = "총점을 선택해주세요.")
    @Min(value = 1, message = "별점은 최소 1점입니다.")
    @Max(value = 5, message = "별점은 최대 5점입니다.")
    @Schema(description = "별점 (1~5)", example = "4")
    private Integer rating;

    @NotNull(message = "학점 비율 평가를 선택해주세요.")
    @Schema(description = "학점 비율")
    private GradeSatisfaction gradeSatisfaction;

    @NotNull(message = "과제량 평가를 선택해주세요.")
    @Schema(description = "과제량")
    private AssignmentAmount assignmentAmount;

    @NotNull(message = "조모임 평가를 선택해주세요.")
    @Schema(description = "조모임(팀플)")
    private TeamProject teamProject;

    @NotNull(message = "시험 횟수 평가를 선택해주세요.")
    @Schema(description = "시험 횟수")
    private ExamCount examCount;

    @NotNull(message = "출결 방식 평가를 선택해주세요.")
    @Schema(description = "출결 방식")
    private AttendanceType attendanceType;

    @NotBlank(message = "후기 내용을 입력해주세요.")
    @Size(min = 20, max = 1000, message = "후기는 최소 20자 이상 1000자 이하로 작성해주세요.")
    @Schema(description = "상세 후기 내용")
    private String content;

    public CourseReviewUpdateDto(
            Integer rating,
            GradeSatisfaction gradeSatisfaction,
            AssignmentAmount assignmentAmount,
            TeamProject teamProject,
            ExamCount examCount,
            AttendanceType attendanceType,
            String content
    ) {
        this.rating = rating;
        this.gradeSatisfaction = gradeSatisfaction;
        this.assignmentAmount = assignmentAmount;
        this.teamProject = teamProject;
        this.examCount = examCount;
        this.attendanceType = attendanceType;
        this.content = content;
    }
}
