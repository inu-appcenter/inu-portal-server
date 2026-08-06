package kr.inuappcenterportal.inuportal.domain.course.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseOfferingMeetingFilter;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingSearchCondition;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.HY_NAME;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.ISU_NAME;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.MeetingFilterMode;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static kr.inuappcenterportal.inuportal.domain.course.model.QCourse.course;
import static kr.inuappcenterportal.inuportal.domain.course.model.QCourseMeeting.courseMeeting;
import static kr.inuappcenterportal.inuportal.domain.course.model.QCourseOffering.courseOffering;
import static kr.inuappcenterportal.inuportal.domain.semester.model.QSemester.semester;

@Repository
public class CourseOfferingRepositoryImpl implements CourseOfferingRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    // @DataJpaTest에서는 전체 설정이 다 올라가지 않기 때문에 QuerydslConfig가 올라가지 않아 JPAQueryFactory Bean이 없음
    // 따라서 Bean 주입대신 EntityManager를 주입시긴다.
    public CourseOfferingRepositoryImpl(EntityManager entityManager) {
        this.jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

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

        if (!condition.meetings().isEmpty()) {
            BooleanBuilder meetingOverlapBuilder = new BooleanBuilder();

            for (CourseOfferingMeetingFilter meeting : condition.meetings()) {
                meetingOverlapBuilder.or(
                        courseMeeting.day.eq(meeting.day())
                                .and(courseMeeting.startTime.lt(meeting.endTime()))
                                .and(courseMeeting.endTime.gt(meeting.startTime()))
                );
            }

            if (condition.filterMode() == MeetingFilterMode.HAS_CLASS) {
                builder.and(
                        JPAExpressions
                                .selectOne()
                                .from(courseMeeting)
                                .where(
                                        courseMeeting.courseOffering.eq(courseOffering),
                                        meetingOverlapBuilder
                                )
                                .exists()
                );
            }

            if (condition.filterMode() == MeetingFilterMode.NO_CLASS) {
                builder.and(
                        JPAExpressions
                                .selectOne()
                                .from(courseMeeting)
                                .where(
                                        courseMeeting.courseOffering.eq(courseOffering),
                                        meetingOverlapBuilder
                                )
                                .notExists()
                );
            }
        }

        // 과목이 전공이면 1, 교양이면 2, 기타면 3
        NumberExpression<Integer> categoryOrder = new CaseBuilder()
                .when(courseOffering.isuName.in(
                        ISU_NAME.MAJOR_ADVANCED,
                        ISU_NAME.MAJOR_CORE,
                        ISU_NAME.MAJOR_FOUNDATION
                )).then(1)
                .when(courseOffering.isuName.in(
                        ISU_NAME.BASIC_LIBERAL_ARTS,
                        ISU_NAME.ADVANCED_LIBERAL_ARTS,
                        ISU_NAME.CORE_LIBERAL_ARTS,
                        ISU_NAME.GENERAL_ELECTIVE,
                        ISU_NAME.MILITARY_SCIENCE,
                        ISU_NAME.TEACHING_PROFESSION
                )).then(2)
                .otherwise(3);

        NumberExpression<Integer> hyNameOrder = new CaseBuilder()
                .when(courseOffering.hyName.eq(HY_NAME.ALL)).then(1)
                .when(courseOffering.hyName.eq(HY_NAME.GRADE1)).then(2)
                .when(courseOffering.hyName.eq(HY_NAME.GRADE2)).then(3)
                .when(courseOffering.hyName.eq(HY_NAME.GRADE3)).then(4)
                .when(courseOffering.hyName.eq(HY_NAME.GRADE4)).then(5)
                .otherwise(99);


        // 위에서 만든 규칙을 SQL처럼 사용하는 곳
        // "select * from courseOffering join course join semester where .. orderBy .. limit .. offset .." 이런 느낌의 쿼리
        // (정렬은 offset/limit 이전에)
        List<CourseOffering> content = jpaQueryFactory
                .selectFrom(courseOffering)
                .join(courseOffering.course, course).fetchJoin()
                .join(courseOffering.semester, semester).fetchJoin()
                .where(builder)
                .orderBy(
                        categoryOrder.asc(),
                        hyNameOrder.asc(),
                        course.title.asc()
                )
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
