package kr.inuappcenterportal.inuportal.domain.firebase.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_fcm_message")
public class MemberFcmMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fcm_message_id", nullable = false)
    private Long fcmMessageId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;
}
