package kr.inuappcenterportal.inuportal.domain.timeTable.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.TimeTableItemType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeTableItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_item_id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TimeTableItemType type;

    private String memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private TimeTable timeTable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id")
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_schedule_id")
    private CustomSchedule customSchedule;

    private TimeTableItem(
            String memo,
            TimeTableItemType type,
            TimeTable timeTable,
            CourseOffering courseOffering,
            CustomSchedule customSchedule
    ) {
        this.memo = memo;
        this.type = type;
        this.timeTable = timeTable;
        this.courseOffering = courseOffering;
        this.customSchedule = customSchedule;
    }


    /**
     * 시간표 요소는 강의 또는 커스텀 일정 중 하나로만 생성되어야 한다
     */
    public static TimeTableItem createForCourse(
            String memo,
            TimeTable timeTable,
            CourseOffering courseOffering
    ) {
        return new TimeTableItem(memo, TimeTableItemType.COURSE, timeTable, courseOffering, null);
    }

    public static TimeTableItem createForCustomSchedule(
            String memo,
            TimeTable timeTable,
            CustomSchedule customSchedule
    ) {
        return new TimeTableItem(memo, TimeTableItemType.CUSTOM, timeTable, null, customSchedule);
    }
}
