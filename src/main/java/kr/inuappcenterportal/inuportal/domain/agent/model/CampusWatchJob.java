package kr.inuappcenterportal.inuportal.domain.agent.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchDomain;
import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 스마트 캠퍼스 실시간 감시(스나이퍼) 작업 엔티티
 * - 힐링존/열람실 빈자리 감시, 도서 반납 감시 등 서버사이드 비동기 폴링 작업 관리
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "campus_watch_job",
        indexes = {
                @Index(name = "idx_watch_domain_status_expires", columnList = "domain, status, expires_at"),
                @Index(name = "idx_watch_member_status", columnList = "member_id, status")
        }
)
public class CampusWatchJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false, length = 30)
    private CampusWatchDomain domain; // LIBRARY_SEAT, BOOK_RETURN

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId; // "HEALING_ZONE", "ROOM_1", "BOOK_1329926"

    @Column(name = "target_name", nullable = false, length = 100)
    private String targetName; // "힐링존", "제1열람실", "운영체제"

    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType; // "AVAILABLE_GT_ZERO", "ON_SHELF"

    @Column(name = "extra_params_json", columnDefinition = "TEXT")
    private String extraParamsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CampusWatchStatus status = CampusWatchStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Builder
    public CampusWatchJob(
            Member member,
            CampusWatchDomain domain,
            String targetId,
            String targetName,
            String conditionType,
            String extraParamsJson,
            LocalDateTime expiresAt
    ) {
        this.member = member;
        this.domain = domain;
        this.targetId = targetId;
        this.targetName = targetName;
        this.conditionType = conditionType;
        this.extraParamsJson = extraParamsJson;
        this.expiresAt = expiresAt != null ? expiresAt : LocalDateTime.now().plusMinutes(90);
        this.status = CampusWatchStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsNotified() {
        this.status = CampusWatchStatus.NOTIFIED;
        this.notifiedAt = LocalDateTime.now();
    }

    public void markAsExpired() {
        this.status = CampusWatchStatus.EXPIRED;
    }

    public void cancel() {
        this.status = CampusWatchStatus.CANCELLED;
    }

    public boolean isExpired(LocalDateTime now) {
        return this.expiresAt != null && this.expiresAt.isBefore(now);
    }
}
