package kr.inuappcenterportal.inuportal.global.logging.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "summary_member_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryMemberLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    @Column(nullable = false)
    private LocalDate date;

    private SummaryMemberLog(Integer memberCount, LocalDate date) {
        this.memberCount = memberCount;
        this.date = date;
    }

    public static SummaryMemberLog of(Integer memberCount, LocalDate date) {
        return new SummaryMemberLog(memberCount, date);
    }
}
