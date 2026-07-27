package kr.inuappcenterportal.inuportal.domain.timeTable.repository;

import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;
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

}
