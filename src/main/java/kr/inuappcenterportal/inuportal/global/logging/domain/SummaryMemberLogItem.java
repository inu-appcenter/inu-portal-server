package kr.inuappcenterportal.inuportal.global.logging.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "summary_member_log_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryMemberLogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_member_log_id", nullable = false)
    private Long summaryMemberLogId;

    @Column(nullable = false, length = 30)
    private String memberId;

    private SummaryMemberLogItem(Long summaryMemberLogId, String memberId) {
        this.summaryMemberLogId = summaryMemberLogId;
        this.memberId = memberId;
    }

    public static SummaryMemberLogItem of(Long summaryMemberLogId, String memberId) {
        return new SummaryMemberLogItem(summaryMemberLogId, memberId);
    }
}
