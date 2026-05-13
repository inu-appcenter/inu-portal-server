package kr.inuappcenterportal.inuportal.domain.chat.domain;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatMemberStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_room_member")
public class ChatRoomMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMemberStatus status;

    @Builder
    public ChatRoomMember(ChatRoom chatRoom, Member member) {
        this.chatRoom = chatRoom;
        this.member = member;
        this.status = ChatMemberStatus.JOINED;
        this.lastReadMessageId = 0L;
    }

    public void leave() {
        this.status = ChatMemberStatus.LEFT;
    }

    public void updateLastReadMessageId(Long messageId) {
        if (this.lastReadMessageId == null || messageId > this.lastReadMessageId) {
            this.lastReadMessageId = messageId;
        }
    }

    public void rejoin() {
        this.status = ChatMemberStatus.JOINED;
    }
}
