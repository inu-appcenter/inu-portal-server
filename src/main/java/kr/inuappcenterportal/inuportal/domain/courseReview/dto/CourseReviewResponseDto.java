package kr.inuappcenterportal.inuportal.domain.courseReview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.courseReview.enums.*;
import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReview;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "강의평 응답 DTO")
public class CourseReviewResponseDto {

    @Schema(description = "강의평 ID", example = "1")
    private Long id;

    @Schema(description = "교과목 ID", example = "12")
    private Long courseId;

    @Schema(description = "교과목명", example = "자료구조")
    private String courseTitle;

    @Schema(description = "개설 강좌 ID", example = "105")
    private Long courseOfferingId;

    @Schema(description = "담당 교수명", example = "홍길동")
    private String professor;

    @Schema(description = "수강 연도", example = "2026")
    private Integer year;

    @Schema(description = "수강 학기", example = "1")
    private Integer term;

    @Schema(description = "학기 라벨", example = "2026-1학기")
    private String semesterLabel;

    @Schema(description = "별점 (1~5)", example = "5")
    private Integer rating;

    @Schema(description = "학점 비율")
    private GradeSatisfaction gradeSatisfaction;

    @Schema(description = "학점 비율 텍스트", example = "너그러움")
    private String gradeSatisfactionText;

    @Schema(description = "과제량")
    private AssignmentAmount assignmentAmount;

    @Schema(description = "과제량 텍스트", example = "보통")
    private String assignmentAmountText;

    @Schema(description = "조모임")
    private TeamProject teamProject;

    @Schema(description = "조모임 텍스트", example = "없음")
    private String teamProjectText;

    @Schema(description = "시험 횟수")
    private ExamCount examCount;

    @Schema(description = "시험 횟수 텍스트", example = "2회")
    private String examCountText;

    @Schema(description = "출결 방식")
    private AttendanceType attendanceType;

    @Schema(description = "출결 방식 텍스트", example = "전자출결")
    private String attendanceTypeText;

    @Schema(description = "상세 후기")
    private String content;

    @Schema(description = "수강생 인증 여부", example = "true")
    private boolean isVerified;

    @Schema(description = "추천 수", example = "4")
    private int likeCount;

    @Schema(description = "내가 추천했는지 여부", example = "false")
    private boolean isLiked;

    @Schema(description = "내가 작성한 후기인지 여부", example = "false")
    private boolean isMyReview;

    @Schema(description = "작성자 표시 (익명)", example = "23학번")
    private String writerDisplay;

    @Schema(description = "작성일시")
    private LocalDateTime createdAt;

    public static CourseReviewResponseDto of(CourseReview review, Long currentMemberId, boolean isLiked) {
        boolean isMyReview = currentMemberId != null && review.getMember().getId().equals(currentMemberId);
        String writer = "익명";
        if (review.getMember() != null && review.getMember().getStudentId() != null) {
            String sid = review.getMember().getStudentId().trim();
            if (sid.length() >= 4) {
                writer = sid.substring(2, 4) + "학번";
            }
        }

        String termStr = review.getTerm() == 1 ? "1학기" :
                review.getTerm() == 2 ? "2학기" :
                        review.getTerm() == 3 ? "여름학기" : "겨울학기";
        String semesterLabel = review.getYear() + "-" + termStr;

        return CourseReviewResponseDto.builder()
                .id(review.getId())
                .courseId(review.getCourse().getId())
                .courseTitle(review.getCourse().getTitle())
                .courseOfferingId(review.getCourseOffering() != null ? review.getCourseOffering().getId() : null)
                .professor(review.getProfessor())
                .year(review.getYear())
                .term(review.getTerm())
                .semesterLabel(semesterLabel)
                .rating(review.getRating())
                .gradeSatisfaction(review.getGradeSatisfaction())
                .gradeSatisfactionText(review.getGradeSatisfaction().getDescription())
                .assignmentAmount(review.getAssignmentAmount())
                .assignmentAmountText(review.getAssignmentAmount().getDescription())
                .teamProject(review.getTeamProject())
                .teamProjectText(review.getTeamProject().getDescription())
                .examCount(review.getExamCount())
                .examCountText(review.getExamCount().getDescription())
                .attendanceType(review.getAttendanceType())
                .attendanceTypeText(review.getAttendanceType().getDescription())
                .content(review.getContent())
                .isVerified(review.isVerified())
                .likeCount(review.getLikeCount())
                .isLiked(isLiked)
                .isMyReview(isMyReview)
                .writerDisplay(writer)
                .createdAt(review.getCreateDate())
                .build();
    }
}
