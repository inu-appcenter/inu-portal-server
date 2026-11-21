package kr.inuappcenterportal.inuportal.domain.courseReview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@Schema(description = "강의평 요약 통계 DTO")
public class CourseReviewSummaryDto {

    @Schema(description = "교과목 ID", example = "12")
    private Long courseId;

    @Schema(description = "교과목명", example = "자료구조")
    private String courseTitle;

    @Schema(description = "조회 대상 교수명 (전체일 경우 null)", example = "홍길동")
    private String professor;

    @Schema(description = "총 후기 수", example = "18")
    private long totalReviews;

    @Schema(description = "평균 별점 (1.0~5.0)", example = "4.3")
    private double averageRating;

    @Schema(description = "별점대별 개수 (키: 1~5, 값: 건수)")
    private Map<Integer, Long> ratingDistribution;

    @Schema(description = "학점 비율 백분율 (키: 너그러움/보통/깐깐함, 값: 0~100)")
    private Map<String, Double> gradeSatisfactionRatios;

    @Schema(description = "과제량 백분율 (키: 많음/보통/적음, 값: 0~100)")
    private Map<String, Double> assignmentRatios;

    @Schema(description = "조모임 백분율 (키: 많음/보통/없음, 값: 0~100)")
    private Map<String, Double> teamProjectRatios;

    @Schema(description = "시험 횟수 백분율 (키: 2회/1회/과제대체/없음, 값: 0~100)")
    private Map<String, Double> examCountRatios;

    @Schema(description = "출결 방식 백분율 (키: 전자출결/직접호명/혼합/반영안함, 값: 0~100)")
    private Map<String, Double> attendanceRatios;

    @Schema(description = "대표 키워드 뱃지", example = "학점 너그러움")
    private String representativeKeyword;
}
