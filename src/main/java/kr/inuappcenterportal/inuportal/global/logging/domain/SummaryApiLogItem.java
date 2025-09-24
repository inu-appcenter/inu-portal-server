package kr.inuappcenterportal.inuportal.global.logging.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "summary_api_log_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryApiLogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_api_log_id", nullable = false)
    private Long summaryApiLogId;

    @Column(name = "api_count", nullable = false)
    private Long apiCount;

    @Column(nullable = false, length = 50)
    private String method;

    @Column(nullable = false, length = 50)
    private String uri;

    private SummaryApiLogItem(Long summaryApiLogId, Long apiCount, String method, String uri) {
        this.summaryApiLogId = summaryApiLogId;
        this.apiCount = apiCount;
        this.method = method;
        this.uri = uri;
    }

    public static SummaryApiLogItem of(Long summaryApiLogId, Long apiCount, String method, String uri) {
        return new SummaryApiLogItem(summaryApiLogId, apiCount, method, uri);
    }
}
