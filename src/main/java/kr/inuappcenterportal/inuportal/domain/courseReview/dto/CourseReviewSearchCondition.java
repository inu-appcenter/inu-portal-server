package kr.inuappcenterportal.inuportal.domain.courseReview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.courseReview.enums.CourseReviewSort;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "강의평 목록 조회 조건")
public class CourseReviewSearchCondition {

    @Schema(description = "특정 교수명 필터", example = "홍길동")
    private String professor;

    @Schema(description = "정렬 방식 (LATEST: 최신순, LIKES: 추천순, RATING_DESC: 별점 높은순, RATING_ASC: 별점 낮은순)", defaultValue = "LATEST")
    private CourseReviewSort sort = CourseReviewSort.LATEST;

    @Schema(description = "수강 인증 리뷰만 보기 여부", defaultValue = "false")
    private Boolean verifiedOnly = false;
}
