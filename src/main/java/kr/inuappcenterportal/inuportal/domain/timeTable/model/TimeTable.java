package kr.inuappcenterportal.inuportal.domain.timeTable.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_id", nullable = false)
    private Long id;

    @Column(name = "timetable_name", nullable = false)
    private String timeTableName;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(nullable = false)
    private Visibility visibility;
}
