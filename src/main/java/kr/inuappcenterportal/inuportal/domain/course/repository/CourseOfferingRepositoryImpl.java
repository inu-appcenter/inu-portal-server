package kr.inuappcenterportal.inuportal.domain.course.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseOfferingMeetingFilter;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingSearchCondition;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.CourseOfferingSort;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.MeetingFilterMode;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static kr.inuappcenterportal.inuportal.domain.course.model.QCourse.course;
import static kr.inuappcenterportal.inuportal.domain.course.model.QCourseMeeting.courseMeeting;
import static kr.inuappcenterportal.inuportal.domain.course.model.QCourseOffering.courseOffering;
import static kr.inuappcenterportal.inuportal.domain.semester.model.QSemester.semester;
import static kr.inuappcenterportal.inuportal.domain.timeTable.model.QTimeTableItem.timeTableItem;

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
            builder.and(courseOffering.deptCode.in(condition.deptName().apiCodes()));
        }

        if (condition.collegeName() != null) {
            builder.and(courseOffering.collegeCode.in(condition.collegeName().apiCodes()));
        }

        if (!condition.hyNames().isEmpty()) {
            builder.and(courseOffering.hyNameRaw.in(condition.hyNames()));
        }

        if (!condition.isuNames().isEmpty()) {
            builder.and(courseOffering.isuNameRaw.in(condition.isuNames()));
        }

        if (!condition.isuFldNames().isEmpty()) {
            builder.and(courseOffering.isuFldNameRaw.in(condition.isuFldNames()));
        }

        if (!condition.ssupTypeNames().isEmpty()) {
            builder.and(courseOffering.ssupTypeNameRaw.in(condition.ssupTypeNames()));
        }

        if (!condition.credits().isEmpty()) {
            builder.and(courseOffering.credit.in(condition.credits()));
        }

        if (StringUtils.hasText(condition.keyword())) {
            builder.and(
                    courseOffering.subjectNumber.contains(condition.keyword())
                            .or(courseOffering.course.title.contains(condition.keyword()))
                            .or(courseOffering.course.englishTitle.contains(condition.keyword()))
                            .or(courseOffering.professor.contains(condition.keyword()))
            );
        }

        if (!condition.meetings().isEmpty()) {
            BooleanBuilder meetingOverlapBuilderNoClass = new BooleanBuilder();
            BooleanBuilder meetingWithinBuilderHasClass = new BooleanBuilder();

            // 해당 수업 시간이 선택 시간대와 겹치나
            for (CourseOfferingMeetingFilter meeting : condition.meetings()) {
                meetingOverlapBuilderNoClass.or(
                        courseMeeting.day.eq(meeting.day())
                                .and(courseMeeting.startTime.lt(meeting.endTime()))
                                .and(courseMeeting.endTime.gt(meeting.startTime()))
                );
            }

            // 해당 수업 시간이 선택 시간대 안에 완전히 들어가나
            for (CourseOfferingMeetingFilter meeting : condition.meetings()) {
                meetingWithinBuilderHasClass.or(
                        courseMeeting.day.eq(meeting.day())
                                .and(courseMeeting.startTime.goe(meeting.startTime()))
                                .and(courseMeeting.endTime.loe(meeting.endTime()))
                );
            }

            // 듣고 싶은 시간대 선택 모드: 모든 수업 시간이 선택한 시간대 안에 들어오는 강의만
            // 예를 들어, A수업의 시간대가 월 10-12/수 10-12이고, B수업의 시간대가 수 9-10일때 선택한 시간대에 수 9-14라면
            // B수업의 시간대는 선택한 시간대에 온전히 들어오므로 검색이 되지만, A수업의 경우 수요일 수업은 시간대에 들어오지만 월요일 수업은 들어오지 않기 때문에 검색이 되지 않음
            if (condition.filterMode() == MeetingFilterMode.HAS_CLASS) {
                // 선택한 시간대 안에 온전히 들어오나?
                builder.and(
                        JPAExpressions
                                .selectOne()
                                .from(courseMeeting)
                                .where(
                                        courseMeeting.courseOffering.eq(courseOffering),
                                        meetingWithinBuilderHasClass
                                )
                                .exists()
                );
                // 선택한 시간에 안에 들어오지 않는 것이 있나?
                builder.and(
                        JPAExpressions
                                .selectOne()
                                .from(courseMeeting)
                                .where(
                                        courseMeeting.courseOffering.eq(courseOffering),
                                        new BooleanBuilder(meetingWithinBuilderHasClass).not()
                                )
                                .notExists()
                );
            }

            // 선택한 시간대와 조금이라도 겹치는 수업 시간이 하나도 없는 강의만 (수업 시간이 없는 강의도 포함)
            if (condition.filterMode() == MeetingFilterMode.NO_CLASS) {
                // notExists이므로 선택된 시간대를 제외한 모든 시간대를 검색
                builder.and(
                        JPAExpressions
                                .selectOne()
                                .from(courseMeeting)
                                .where(
                                        courseMeeting.courseOffering.eq(courseOffering),
                                        meetingOverlapBuilderNoClass
                                )
                                .notExists()
                );
            }
        }

        // 과목이 전공이면 1, 교양이면 2, 기타면 3
        NumberExpression<Integer> categoryOrder = new CaseBuilder()
                .when(courseOffering.isuCode.in("25", "31", "41")).then(1)
                .when(courseOffering.isuCode.in("11", "21", "23", "50", "70", "80")).then(2)
                .otherwise(3);

        NumberExpression<Integer> hyNameOrder = new CaseBuilder()
                .when(courseOffering.hyCode.eq("0")).then(1)
                .when(courseOffering.hyCode.eq("1")).then(2)
                .when(courseOffering.hyCode.eq("2")).then(3)
                .when(courseOffering.hyCode.eq("3")).then(4)
                .when(courseOffering.hyCode.eq("4")).then(5)
                .otherwise(99);

        JPQLQuery<Long> savedCountSubquery = JPAExpressions
                .select(timeTableItem.timeTable.member.id.countDistinct())
                .from(timeTableItem)
                .where(timeTableItem.courseOffering.eq(courseOffering));

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        if (condition.sort() == CourseOfferingSort.SAVED_COUNT_DESC) {
            orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, savedCountSubquery));
        } else if (condition.sort() == CourseOfferingSort.SAVED_COUNT_ASC) {
            orderSpecifiers.add(new OrderSpecifier<>(Order.ASC, savedCountSubquery));
        }

        orderSpecifiers.add(categoryOrder.asc());
        orderSpecifiers.add(hyNameOrder.asc());
        orderSpecifiers.add(course.title.asc());

        // 위에서 만든 규칙을 SQL처럼 사용하는 곳
        // "select * from courseOffering join course join semester where .. orderBy .. limit .. offset .." 이런 느낌의 쿼리
        // (정렬은 offset/limit 이전에)
        List<CourseOffering> content = jpaQueryFactory
                .selectFrom(courseOffering)
                .join(courseOffering.course, course).fetchJoin()
                .join(courseOffering.semester, semester).fetchJoin()
                .where(builder)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
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
