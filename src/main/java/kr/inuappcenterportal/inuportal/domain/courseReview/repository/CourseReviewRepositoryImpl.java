package kr.inuappcenterportal.inuportal.domain.courseReview.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.CourseReviewSearchCondition;
import kr.inuappcenterportal.inuportal.domain.courseReview.dto.CourseReviewSummaryDto;
import kr.inuappcenterportal.inuportal.domain.courseReview.enums.*;
import kr.inuappcenterportal.inuportal.domain.courseReview.model.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static kr.inuappcenterportal.inuportal.domain.courseReview.model.QCourseReview.courseReview;
import static kr.inuappcenterportal.inuportal.domain.member.model.QMember.member;

@Repository
public class CourseReviewRepositoryImpl implements CourseReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public CourseReviewRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<CourseReview> searchReviews(Long courseId, CourseReviewSearchCondition condition, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(courseReview.course.id.eq(courseId));
        builder.and(courseReview.status.eq(ReviewStatus.ACTIVE));

        if (StringUtils.hasText(condition.getProfessor()) && !"all".equalsIgnoreCase(condition.getProfessor())) {
            builder.and(courseReview.professor.eq(condition.getProfessor().trim()));
        }

        if (Boolean.TRUE.equals(condition.getVerifiedOnly())) {
            builder.and(courseReview.isVerified.isTrue());
        }

        List<OrderSpecifier<?>> orderSpecifiers = getOrderSpecifiers(condition.getSort());

        List<CourseReview> content = queryFactory
                .selectFrom(courseReview)
                .leftJoin(courseReview.member, member).fetchJoin()
                .where(builder)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(courseReview.count())
                .from(courseReview)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public CourseReviewSummaryDto getCourseReviewSummary(Course course, String professor) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(courseReview.course.id.eq(course.getId()));
        builder.and(courseReview.status.eq(ReviewStatus.ACTIVE));

        boolean hasProfessor = StringUtils.hasText(professor) && !"all".equalsIgnoreCase(professor);
        if (hasProfessor) {
            builder.and(courseReview.professor.eq(professor.trim()));
        }

        List<CourseReview> reviews = queryFactory
                .selectFrom(courseReview)
                .where(builder)
                .fetch();

        long total = reviews.size();
        if (total == 0) {
            return CourseReviewSummaryDto.builder()
                    .courseId(course.getId())
                    .courseTitle(course.getTitle())
                    .professor(hasProfessor ? professor.trim() : null)
                    .totalReviews(0)
                    .averageRating(0.0)
                    .ratingDistribution(createEmptyDistribution())
                    .gradeSatisfactionRatios(Collections.emptyMap())
                    .assignmentRatios(Collections.emptyMap())
                    .teamProjectRatios(Collections.emptyMap())
                    .examCountRatios(Collections.emptyMap())
                    .attendanceRatios(Collections.emptyMap())
                    .representativeKeyword(null)
                    .build();
        }

        double rawAvg = reviews.stream().mapToInt(CourseReview::getRating).average().orElse(0.0);
        double avgRating = BigDecimal.valueOf(rawAvg).setScale(1, RoundingMode.HALF_UP).doubleValue();

        // 1. 별점 분포 계산
        Map<Integer, Long> distribution = new TreeMap<>(Collections.reverseOrder());
        for (int i = 5; i >= 1; i--) {
            int ratingVal = i;
            long count = reviews.stream().filter(r -> r.getRating() == ratingVal).count();
            distribution.put(ratingVal, count);
        }

        // 2. 5대 지표별 비율 계산
        Map<String, Double> gradeRatios = calculateRatio(
                reviews.stream().collect(Collectors.groupingBy(r -> r.getGradeSatisfaction().getDescription(), Collectors.counting())),
                total
        );

        Map<String, Double> assignmentRatios = calculateRatio(
                reviews.stream().collect(Collectors.groupingBy(r -> r.getAssignmentAmount().getDescription(), Collectors.counting())),
                total
        );

        Map<String, Double> teamProjectRatios = calculateRatio(
                reviews.stream().collect(Collectors.groupingBy(r -> r.getTeamProject().getDescription(), Collectors.counting())),
                total
        );

        Map<String, Double> examCountRatios = calculateRatio(
                reviews.stream().collect(Collectors.groupingBy(r -> r.getExamCount().getDescription(), Collectors.counting())),
                total
        );

        Map<String, Double> attendanceRatios = calculateRatio(
                reviews.stream().collect(Collectors.groupingBy(r -> r.getAttendanceType().getDescription(), Collectors.counting())),
                total
        );

        // 3. 대표 키워드 도출
        String representativeKeyword = determineRepresentativeKeyword(gradeRatios, assignmentRatios, teamProjectRatios, examCountRatios);

        return CourseReviewSummaryDto.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .professor(hasProfessor ? professor.trim() : null)
                .totalReviews(total)
                .averageRating(avgRating)
                .ratingDistribution(distribution)
                .gradeSatisfactionRatios(gradeRatios)
                .assignmentRatios(assignmentRatios)
                .teamProjectRatios(teamProjectRatios)
                .examCountRatios(examCountRatios)
                .attendanceRatios(attendanceRatios)
                .representativeKeyword(representativeKeyword)
                .build();
    }

    private Map<Integer, Long> createEmptyDistribution() {
        Map<Integer, Long> dist = new TreeMap<>(Collections.reverseOrder());
        for (int i = 5; i >= 1; i--) {
            dist.put(i, 0L);
        }
        return dist;
    }

    private Map<String, Double> calculateRatio(Map<String, Long> countMap, long total) {
        Map<String, Double> ratios = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : countMap.entrySet()) {
            double percent = (entry.getValue() * 100.0) / total;
            double rounded = BigDecimal.valueOf(percent).setScale(1, RoundingMode.HALF_UP).doubleValue();
            ratios.put(entry.getKey(), rounded);
        }
        return ratios;
    }

    private String determineRepresentativeKeyword(
            Map<String, Double> gradeRatios,
            Map<String, Double> assignmentRatios,
            Map<String, Double> teamProjectRatios,
            Map<String, Double> examCountRatios
    ) {
        if (gradeRatios.getOrDefault("너그러움", 0.0) >= 50.0) {
            return "학점 너그러움";
        }
        if (assignmentRatios.getOrDefault("적음", 0.0) >= 50.0) {
            return "과제 적음";
        }
        if (teamProjectRatios.getOrDefault("없음", 0.0) >= 60.0) {
            return "팀플 없음";
        }
        if (gradeRatios.getOrDefault("깐깐함", 0.0) >= 50.0) {
            return "학점 깐깐함";
        }
        if (examCountRatios.getOrDefault("과제대체", 0.0) >= 50.0) {
            return "시험 과제대체";
        }
        return null;
    }

    private List<OrderSpecifier<?>> getOrderSpecifiers(CourseReviewSort sort) {
        List<OrderSpecifier<?>> list = new ArrayList<>();
        if (sort == null) {
            sort = CourseReviewSort.LATEST;
        }

        switch (sort) {
            case LIKES -> {
                list.add(courseReview.likeCount.desc());
                list.add(courseReview.createDate.desc());
            }
            case RATING_DESC -> {
                list.add(courseReview.rating.desc());
                list.add(courseReview.createDate.desc());
            }
            case RATING_ASC -> {
                list.add(courseReview.rating.asc());
                list.add(courseReview.createDate.desc());
            }
            default -> list.add(courseReview.createDate.desc());
        }
        return list;
    }
}
