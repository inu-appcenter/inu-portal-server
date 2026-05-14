package kr.inuappcenterportal.inuportal.domain.member.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "friend")
public class Friend extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Member requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendStatus status;

    @Column(name = "requester_alias")
    private String requesterAlias;

    @Column(name = "receiver_alias")
    private String receiverAlias;

    @Builder
    public Friend(Member requester, Member receiver, FriendStatus status) {
        this.requester = requester;
        this.receiver = receiver;
        this.status = status;
    }

    public void accept() {
        this.status = FriendStatus.ACCEPTED;
    }

    public void updateAlias(Long memberId, String alias) {
        if (this.requester.getId().equals(memberId)) {
            this.requesterAlias = alias;
        } else if (this.receiver.getId().equals(memberId)) {
            this.receiverAlias = alias;
        }
    }
}
