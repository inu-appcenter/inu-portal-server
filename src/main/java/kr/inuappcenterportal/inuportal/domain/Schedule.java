package kr.inuappcenterportal.inuportal.domain;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column
    private String content;

    @Builder
    public Schedule (LocalDate startDate, LocalDate endDate, String content){
        this.startDate =startDate;
        this.endDate = endDate;
        this.content = content;
    }




}
