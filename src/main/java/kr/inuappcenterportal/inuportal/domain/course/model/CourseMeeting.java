package kr.inuappcenterportal.inuportal.domain.course.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "course_meeting")
public class CourseMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_meeting_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    private String location;

    @Column(name = "meeting_sequence", nullable = false)
    private String sequence;

    @Column(name = "lectm_code")
    private String lectmCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "`day`")
    private DayOfWeek day;


    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    private CourseMeeting(
            CourseOffering courseOffering,
            String location,
            String sequence,
            String lectmCode,
            DayOfWeek day,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.courseOffering = courseOffering;
        this.location = location;
        this.sequence = sequence;
        this.lectmCode = lectmCode;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static CourseMeeting create(
            CourseOffering courseOffering,
            String location,
            String sequence,
            String lectmCode,
            DayOfWeek day,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return new CourseMeeting(courseOffering, location, sequence, lectmCode, day, startTime, endTime);
    }
}
