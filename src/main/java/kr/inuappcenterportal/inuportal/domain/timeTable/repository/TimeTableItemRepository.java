package kr.inuappcenterportal.inuportal.domain.timeTable.repository;

import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TimeTableItemRepository extends JpaRepository<TimeTableItem, Long> {
    Optional<TimeTableItem> findByCustomScheduleId(Long customScheduleId);

    List<TimeTableItem> findAllByTimeTableId(Long timeTableId);

    boolean existsByTimeTableIdAndCourseOfferingId(Long timeTableId, Long courseOfferingId);

    @Query("""
            select case when count(item) > 0 then true else false end
            from TimeTableItem item
            where item.timeTable.id = :timeTableId
              and (:excludeTimeTableItemId is null or item.id <> :excludeTimeTableItemId)
              and (
                exists (
                  select 1
                  from CourseMeeting meeting
                  where meeting.courseOffering = item.courseOffering
                    and meeting.day = :day
                    and meeting.startTime < :endTime
                    and meeting.endTime > :startTime
                )
                or exists (
                  select 1
                  from CustomScheduleMeeting meeting
                  where meeting.customSchedule = item.customSchedule
                    and meeting.day = :day
                    and meeting.startTime < :endTime
                    and meeting.endTime > :startTime
                )
              )
            """)
    boolean existsOverlappingMeeting(
            @Param("timeTableId") Long timeTableId,
            @Param("day") DayOfWeek day,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeTimeTableItemId") Long excludeTimeTableItemId
    );

    @Query("""
            select case when count(item) > 0 then true else false end
            from TimeTableItem item
            where item.timeTable.id = :timeTableId
              and (
                exists (
                  select 1
                  from CourseMeeting meeting
                  where meeting.courseOffering = item.courseOffering
                    and meeting.day = :day
                    and meeting.startTime < :endTime
                    and meeting.endTime > :startTime
                )
                or exists (
                  select 1
                  from CustomScheduleMeeting meeting
                  where meeting.customSchedule = item.customSchedule
                    and meeting.day = :day
                    and meeting.startTime < :endTime
                    and meeting.endTime > :startTime
                )
              )
            """)
    boolean existsOverlappingMeeting(
            @Param("timeTableId") Long timeTableId,
            @Param("day") DayOfWeek day,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("""
            select item.courseOffering.id, count(distinct item.timeTable.member.id)
            from TimeTableItem item
            where item.courseOffering.id in :courseOfferingIds
            group by item.courseOffering.id
            """)
    List<Object[]> countDistinctMemberByCourseOfferingIdIn(@Param("courseOfferingIds") List<Long> courseOfferingIds);
}
