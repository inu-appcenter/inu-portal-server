package kr.inuappcenterportal.inuportal.domain.customSchedule.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
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
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 빨라야합니다.");
        }

        return new CustomScheduleMeeting(customSchedule, location, day, startTime, endTime);
    }

    public void update(
            String location, DayOfWeek day, LocalTime startTime, LocalTime endTime
    ) {
        if (day == null) {
            throw new IllegalArgumentException("요일은 필수입니다.");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("시작 시간과 종료 시간은 필수입니다.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 빨라야 합니다.");
        }
        
        this.location = location;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
