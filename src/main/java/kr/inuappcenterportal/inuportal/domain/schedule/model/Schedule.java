package kr.inuappcenterportal.inuportal.domain.schedule.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "schedule")
public class Schedule {
    @Id
    private Long id;

    @Column(name="start_date")
    private LocalDate startDate;

    @Column(name="end_date")
    private LocalDate endDate;

    @Column
    private String content;

    @Builder
    public Schedule (Long id, LocalDate startDate, LocalDate endDate, String content){
        this.id = id;
        this.startDate =startDate;
        this.endDate = endDate;
        this.content = content;
    }




}
