package kr.inuappcenterportal.inuportal.domain.courseReview.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.*;
import kr.inuappcenterportal.inuportal.domain.courseReview.service.CourseReviewService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CourseReviewController implements CourseReviewApiSpecification {

    private final CourseReviewService courseReviewService;

    @Override
    @GetMapping("/api/courses/{courseId}/reviews")
    public ResponseEntity<ResponseDto<Page<CourseReviewResponseDto>>> getCourseReviews(
            @PathVariable Long courseId,
            CourseReviewSearchCondition condition,
            @PageableDefault(size = 10, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Member member
    ) {
        Page<CourseReviewResponseDto> page = courseReviewService.getReviews(courseId, condition, pageable, member);
        return ResponseEntity.ok(ResponseDto.of(page, "강의평 목록 조회 성공"));
    }

    @Override
    @GetMapping("/api/courses/{courseId}/reviews/summary")
    public ResponseEntity<ResponseDto<CourseReviewSummaryDto>> getCourseReviewSummary(
            @PathVariable Long courseId,
            @RequestParam(required = false) String professor
    ) {
        CourseReviewSummaryDto summary = courseReviewService.getSummary(courseId, professor);
        return ResponseEntity.ok(ResponseDto.of(summary, "강의평 요약 통계 조회 성공"));
    }

    @Override
    @PostMapping("/api/courses/{courseId}/reviews")
    public ResponseEntity<ResponseDto<CourseReviewResponseDto>> createCourseReview(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseReviewCreateDto dto,
            @AuthenticationPrincipal Member member
    ) {
        CourseReviewResponseDto created = courseReviewService.createReview(courseId, dto, member);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.of(created, "강의평이 등록되었습니다."));
    }

    @Override
    @PutMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ResponseDto<CourseReviewResponseDto>> updateCourseReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody CourseReviewUpdateDto dto,
            @AuthenticationPrincipal Member member
    ) {
        CourseReviewResponseDto updated = courseReviewService.updateReview(reviewId, dto, member);
        return ResponseEntity.ok(ResponseDto.of(updated, "강의평이 수정되었습니다."));
    }

    @Override
    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ResponseDto<Void>> deleteCourseReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Member member
    ) {
        courseReviewService.deleteReview(reviewId, member);
        return ResponseEntity.ok(ResponseDto.of(null, "강의평이 삭제되었습니다."));
    }

    @Override
    @PostMapping("/api/reviews/{reviewId}/like")
    public ResponseEntity<ResponseDto<Map<String, Object>>> toggleLike(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Member member
    ) {
        boolean liked = courseReviewService.toggleLike(reviewId, member);
        return ResponseEntity.ok(ResponseDto.of(Map.of("liked", liked), liked ? "추천되었습니다." : "추천이 취소되었습니다."));
    }

    @Override
    @PostMapping("/api/reviews/{reviewId}/report")
    public ResponseEntity<ResponseDto<Void>> reportReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Member member
    ) {
        String reason = body.getOrDefault("reason", "기타");
        String detail = body.getOrDefault("detail", "");
        courseReviewService.reportReview(reviewId, reason, detail, member);
        return ResponseEntity.ok(ResponseDto.of(null, "신고가 접수되었습니다."));
    }

    @Override
    @GetMapping("/api/my/course-reviews")
    public ResponseEntity<ResponseDto<List<CourseReviewResponseDto>>> getMyReviews(
            @AuthenticationPrincipal Member member
    ) {
        List<CourseReviewResponseDto> myReviews = courseReviewService.getMyReviews(member);
        return ResponseEntity.ok(ResponseDto.of(myReviews, "내 강의평 목록 조회 성공"));
    }
}
