package kr.inuappcenterportal.inuportal.domain.member.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 친구추가 URL/QR에 실리는 초대 코드.
 * 닉네임(학번일 수 있음)을 URL에 노출하지 않기 위해, 회원 정보와 아무런 수학적 연관이 없는
 * 난수 코드를 발급해 이 테이블로만 회원과 연결한다.
 * 회원당 여러 행을 가질 수 있고(1:N), 재발급 시 기존 행을 revoke 처리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "friend_invite_code",
        indexes = {
                @Index(name = "idx_friend_invite_code_member", columnList = "member_id"),
                @Index(name = "idx_friend_invite_code_code", columnList = "code", unique = true)
        }
)
public class FriendInviteCode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    /** null 이면 유효한 코드. 재발급 시 시각이 기록되며 그 즉시 링크가 무효화된다. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Builder
    public FriendInviteCode(Member member, String code) {
        this.member = member;
        this.code = code;
    }

    public void revoke(LocalDateTime now) {
        this.revokedAt = now;
    }

    public boolean isRevoked() {
        return this.revokedAt != null;
    }
}
