package kr.inuappcenterportal.inuportal.domain.firebase.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_fcm_message")
public class MemberFcmMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fcm_message_id", nullable = false)
    private Long fcmMessageId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    private MemberFcmMessage(Long fcmMessageId, Long memberId) {
        this.fcmMessageId = fcmMessageId;
        this.memberId = memberId;
    }

    public static MemberFcmMessage of(Long fcmMessageId, Long memberId) {
        return new MemberFcmMessage(fcmMessageId, memberId);
    }
}
