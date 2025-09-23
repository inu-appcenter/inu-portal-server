package kr.inuappcenterportal.inuportal.global.logging.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "summary_api_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    private SummaryApiLog(LocalDate date) {
        this.date = date;
    }

    public static SummaryApiLog from(LocalDate date) {
        return new SummaryApiLog(date);
    }
}
