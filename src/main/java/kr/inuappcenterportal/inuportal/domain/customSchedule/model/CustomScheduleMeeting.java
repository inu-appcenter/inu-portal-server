package kr.inuappcenterportal.inuportal.domain.customSchedule.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "custom_schedule_meeting")
public class CustomScheduleMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_schedule_meeting_id", nullable = false)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_schedule_id", nullable = false)
    CustomSchedule customSchedule;

    @Column(nullable = false)
    String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DayOfWeek day;

    @Column(nullable = false)
    LocalTime startTime;

    @Column(nullable = false)
    LocalTime endTime;

    private CustomScheduleMeeting(
            CustomSchedule customSchedule,
            String location,
            DayOfWeek day,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.customSchedule = customSchedule;
        this.location = location;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // 정적 팩토리 메서드
    public static CustomScheduleMeeting create(
            CustomSchedule customSchedule,
            String location,
            DayOfWeek day,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (day == null) {
            throw new MyException(MyErrorCode.NECESSARY_DAY_OF_WEEK);
        }
        if (startTime == null || endTime == null) {
            throw new MyException(MyErrorCode.NECESSARY_STARTTIME_AND_ENDTIME);
        }
        if (!startTime.isBefore(endTime)) {
            throw new MyException(MyErrorCode.FASTER_THAN_ENDTIME);
        }

        return new CustomScheduleMeeting(customSchedule, location, day, startTime, endTime);
    }

    public void update(
            String location, DayOfWeek day, LocalTime startTime, LocalTime endTime
    ) {
        if (day == null) {
            throw new MyException(MyErrorCode.NECESSARY_DAY_OF_WEEK);
        }
        if (startTime == null || endTime == null) {
            throw new MyException(MyErrorCode.NECESSARY_STARTTIME_AND_ENDTIME);
        }
        if (!startTime.isBefore(endTime)) {
            throw new MyException(MyErrorCode.FASTER_THAN_ENDTIME);
        }

        this.location = location;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
