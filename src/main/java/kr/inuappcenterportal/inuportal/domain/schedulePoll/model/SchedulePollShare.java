package kr.inuappcenterportal.inuportal.domain.schedulePoll.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ShareKind;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 채팅방에 보낸 조율 카드 1장. 같은 방에 같은 카드를 다시 보내면 이 기록의 메시지를 재사용하고,
// 마감 1시간 전 알림은 이 기록이 있는 방들에 보낸다.
// chat_message는 2초마다 모아서 저장되므로 그쪽을 찾지 않고 이 기록을 기준으로 한다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "schedule_poll_share",
        uniqueConstraints = {
                // 같은 방에 같은 카드(조율·종류·버전)는 하나만
                @UniqueConstraint(name = "uk_schedule_poll_share_card_room",
                        columnNames = {"poll_id", "kind", "result_version", "chat_room_id"})
        }
)
public class SchedulePollShare extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ShareKind kind;

    // RESULT 카드의 결과 번호. INVITE 카드는 0
    @Column(name = "result_version", nullable = false)
    private int resultVersion;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "chat_message_id", nullable = false)
    private Long chatMessageId;

    // 카드를 보낸 사람(메시지 발신자)
    @Column(name = "shared_by", nullable = false)
    private Long sharedBy;

    private SchedulePollShare(Long pollId, ShareKind kind, int resultVersion, Long chatRoomId, Long chatMessageId, Long sharedBy) {
        this.pollId = pollId;
        this.kind = kind;
        this.resultVersion = resultVersion;
        this.chatRoomId = chatRoomId;
        this.chatMessageId = chatMessageId;
        this.sharedBy = sharedBy;
    }

    public static SchedulePollShare create(Long pollId, ShareKind kind, int resultVersion, Long chatRoomId,
                                           Long chatMessageId, Long sharedBy) {
        return new SchedulePollShare(pollId, kind, resultVersion, chatRoomId, chatMessageId, sharedBy);
    }
}
