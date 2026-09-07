package kr.inuappcenterportal.inuportal.domain.firebase.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림을 끝내 전달하지 못한 회원. 재시도 대상을 이 집합으로 한정하기 위해 남긴다.
 *
 * <p>왜 필요한가. {@link MemberFcmMessage}는 알림함 행(읽음 여부/조회수)일 뿐 전달 성공 여부를
 * 담지 않고, {@link FcmMessage}는 성공/실패를 합계로만 갖는다. 그래서 이 기록이 없으면 재시도가
 * "원래 대상 전원에게 재발송"이 되고, 이미 받은 사람에게 중복 푸시가 나간다.
 *
 * <p>기록 단위는 <b>토큰이 아니라 회원</b>이다. 한 회원이 기기를 여러 대 쓰면 토큰도 여러 개인데,
 * 그중 하나라도 성공했다면 그 회원은 알림을 받은 것이므로 실패로 남기지 않는다. 반대로 재시도할 때는
 * 저장된 토큰이 아니라 그 회원의 <b>현재</b> 토큰을 다시 조회한다. 발송 실패 사유가 만료된 토큰인
 * 경우가 많아, 옛 토큰으로 다시 보내면 같은 이유로 또 실패하기 때문이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "fcm_message_failed_target",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fcm_message_failed_target",
                columnNames = {"fcm_message_id", "member_id"}
        )
)
public class FcmMessageFailedTarget extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fcm_message_id", nullable = false)
    private Long fcmMessageId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    private FcmMessageFailedTarget(Long fcmMessageId, Long memberId) {
        this.fcmMessageId = fcmMessageId;
        this.memberId = memberId;
    }

    public static FcmMessageFailedTarget of(Long fcmMessageId, Long memberId) {
        return new FcmMessageFailedTarget(fcmMessageId, memberId);
    }
}
