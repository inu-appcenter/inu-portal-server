package kr.inuappcenterportal.inuportal.domain.course.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingSearchCondition;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static kr.inuappcenterportal.inuportal.domain.course.model.QCourse.course;
import static kr.inuappcenterportal.inuportal.domain.course.model.QCourseOffering.courseOffering;
import static kr.inuappcenterportal.inuportal.domain.semester.model.QSemester.semester;

@Repository
@RequiredArgsConstructor
public class CourseOfferingRepositoryImpl implements CourseOfferingRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<CourseOffering> search(CourseOfferingSearchCondition condition, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(courseOffering.semester.id.eq(condition.semesterId()));

        if (condition.deptName() != null) {
            builder.and(courseOffering.deptName.eq(condition.deptName()));
        }

        if (condition.collegeName() != null) {
            builder.and(courseOffering.collegeName.eq(condition.collegeName()));
        }

        if (!condition.hyNames().isEmpty()) {
            builder.and(courseOffering.hyName.in(condition.hyNames()));
        }

        if (!condition.isuNames().isEmpty()) {
            builder.and(courseOffering.isuName.in(condition.isuNames()));
        }

        if (!condition.isuFldNames().isEmpty()) {
            builder.and(courseOffering.isuFldName.in(condition.isuFldNames()));
        }

        if (!condition.ssupTypeNames().isEmpty()) {
            builder.and(courseOffering.ssupTypeName.in(condition.ssupTypeNames()));
        }

        if (!condition.credits().isEmpty()) {
            builder.and(courseOffering.credit.in(condition.credits()));
        }

        if (StringUtils.hasText(condition.keyword())) {
            builder.and(
                    courseOffering.subjectNumber.containsIgnoreCase(condition.keyword())
                            .or(courseOffering.course.title.containsIgnoreCase(condition.keyword()))
                            .or(courseOffering.course.englishTitle.containsIgnoreCase(condition.keyword()))
            );

        }

        List<CourseOffering> content = jpaQueryFactory
                .selectFrom(courseOffering)
                .join(courseOffering.course, course).fetchJoin()
                .join(courseOffering.semester, semester).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(courseOffering.count())
                .from(courseOffering)
                .join(courseOffering.course, course)
                .join(courseOffering.semester, semester)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
