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
@Schema(description = "강의평 작성 요청 DTO")
public class CourseReviewCreateDto {

    @Schema(description = "개설 강좌 ID (선택)", example = "105")
    private Long courseOfferingId;

    @NotBlank(message = "교수명을 입력해주세요.")
    @Schema(description = "담당 교수명", example = "홍길동")
    private String professor;

    @NotNull(message = "수강 연도를 선택해주세요.")
    @Schema(description = "수강 연도", example = "2026")
    private Integer year;

    @NotNull(message = "수강 학기를 선택해주세요.")
    @Schema(description = "수강 학기 (1: 1학기, 2: 2학기, 3: 여름, 4: 겨울)", example = "1")
    private Integer term;

    @NotNull(message = "총점을 선택해주세요.")
    @Min(value = 1, message = "별점은 최소 1점입니다.")
    @Max(value = 5, message = "별점은 최대 5점입니다.")
    @Schema(description = "별점 (1~5)", example = "5")
    private Integer rating;

    @NotNull(message = "학점 비율 평가를 선택해주세요.")
    @Schema(description = "학점 비율 (너그러움/보통/깐깐함)")
    private GradeSatisfaction gradeSatisfaction;

    @NotNull(message = "과제량 평가를 선택해주세요.")
    @Schema(description = "과제량 (많음/보통/적음)")
    private AssignmentAmount assignmentAmount;

    @NotNull(message = "조모임 평가를 선택해주세요.")
    @Schema(description = "조모임(팀플) (많음/보통/없음)")
    private TeamProject teamProject;

    @NotNull(message = "시험 횟수 평가를 선택해주세요.")
    @Schema(description = "시험 횟수 (2회/1회/과제대체/없음)")
    private ExamCount examCount;

    @NotNull(message = "출결 방식 평가를 선택해주세요.")
    @Schema(description = "출결 방식 (전자출결/직접호명/혼합/반영안함)")
    private AttendanceType attendanceType;

    @NotBlank(message = "후기 내용을 입력해주세요.")
    @Size(min = 20, max = 1000, message = "후기는 최소 20자 이상 1000자 이하로 작성해주세요.")
    @Schema(description = "상세 후기 내용 (최소 20자)", example = "강의 설명이 매우 명확하고 과제 피드백을 꼼꼼하게 주셔서 많은 도움이 되었습니다.")
    private String content;

    public CourseReviewCreateDto(
            Long courseOfferingId,
            String professor,
            Integer year,
            Integer term,
            Integer rating,
            GradeSatisfaction gradeSatisfaction,
            AssignmentAmount assignmentAmount,
            TeamProject teamProject,
            ExamCount examCount,
            AttendanceType attendanceType,
            String content
    ) {
        this.courseOfferingId = courseOfferingId;
        this.professor = professor;
        this.year = year;
        this.term = term;
        this.rating = rating;
        this.gradeSatisfaction = gradeSatisfaction;
        this.assignmentAmount = assignmentAmount;
        this.teamProject = teamProject;
        this.examCount = examCount;
        this.attendanceType = attendanceType;
        this.content = content;
    }
}
