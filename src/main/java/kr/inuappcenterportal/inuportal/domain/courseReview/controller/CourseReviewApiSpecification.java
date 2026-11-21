package kr.inuappcenterportal.inuportal.domain.courseReview.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Tag(name = "CourseReview", description = "강의평(강의 후기) 관련 API")
public interface CourseReviewApiSpecification {

    @Operation(summary = "강의평 목록 조회", description = "특정 교과목의 강의평 목록을 정렬 및 필터링하여 페이징 조회합니다.")
    ResponseEntity<ResponseDto<Page<CourseReviewResponseDto>>> getCourseReviews(
            @PathVariable Long courseId,
            CourseReviewSearchCondition condition,
            Pageable pageable,
            Member member
    );

    @Operation(summary = "강의평 요약 통계 조회", description = "특정 교과목 및 교수의 평균 별점, 별점 분포, 5대 세부 지표 비율을 조회합니다.")
    ResponseEntity<ResponseDto<CourseReviewSummaryDto>> getCourseReviewSummary(
            @PathVariable Long courseId,
            @RequestParam(required = false) String professor
    );

    @Operation(summary = "강의평 등록", description = "특정 교과목 및 담당 교수에 대한 강의평을 등록합니다.")
    ResponseEntity<ResponseDto<CourseReviewResponseDto>> createCourseReview(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseReviewCreateDto dto,
            Member member
    );

    @Operation(summary = "강의평 수정", description = "본인이 작성한 강의평을 수정합니다.")
    ResponseEntity<ResponseDto<CourseReviewResponseDto>> updateCourseReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody CourseReviewUpdateDto dto,
            Member member
    );

    @Operation(summary = "강의평 삭제", description = "본인이 작성한 강의평을 삭제합니다.")
    ResponseEntity<ResponseDto<Void>> deleteCourseReview(
            @PathVariable Long reviewId,
            Member member
    );

    @Operation(summary = "강의평 추천(도움돼요) 토글", description = "강의평에 도움돼요를 누르거나 취소합니다.")
    ResponseEntity<ResponseDto<Map<String, Object>>> toggleLike(
            @PathVariable Long reviewId,
            Member member
    );

    @Operation(summary = "강의평 신고 접수", description = "부적절하거나 허위인 강의평을 신고합니다.")
    ResponseEntity<ResponseDto<Void>> reportReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> body,
            Member member
    );

    @Operation(summary = "내가 작성한 강의평 목록 조회", description = "로그인한 사용자가 작성한 강의평 목록을 최신순으로 조회합니다.")
    ResponseEntity<ResponseDto<List<CourseReviewResponseDto>>> getMyReviews(
            Member member
    );

    @Operation(summary = "최근 등록된 강의평 피드 조회", description = "전체 교과목 중 최근에 등록된 공개 강의평 목록을 최신순으로 조회합니다.")
    ResponseEntity<ResponseDto<List<CourseReviewRecentDto>>> getRecentReviews(
            int limit,
            Member member
    );
}
