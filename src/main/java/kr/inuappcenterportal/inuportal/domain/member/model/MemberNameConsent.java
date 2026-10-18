package kr.inuappcenterportal.inuportal.domain.member.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.enums.NameConsentAction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 포털에서 실명을 가져오는 데 대한 동의·철회 기록. 지우지 않고 쌓는다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_name_consent", indexes = {
        @Index(name = "idx_member_name_consent_member", columnList = "member_id")
})
public class MemberNameConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NameConsentAction action;

    // 동의한 약관 버전. 철회면 null
    @Column(name = "terms_version", length = 50)
    private String termsVersion;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    private MemberNameConsent(Long memberId, NameConsentAction action, String termsVersion, LocalDateTime recordedAt) {
        this.memberId = memberId;
        this.action = action;
        this.termsVersion = termsVersion;
        this.recordedAt = recordedAt;
    }

    public static MemberNameConsent agree(Long memberId, String termsVersion, LocalDateTime now) {
        return new MemberNameConsent(memberId, NameConsentAction.AGREE, termsVersion, now);
    }

    public static MemberNameConsent withdraw(Long memberId, LocalDateTime now) {
        return new MemberNameConsent(memberId, NameConsentAction.WITHDRAW, null, now);
    }
}
