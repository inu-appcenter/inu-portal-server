package kr.inuappcenterportal.inuportal.domain.chat.domain;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.chat.enums.MessageType;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_message")
public class ChatMessage extends BaseTimeEntity {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member sender;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "senderNickname", nullable = false)
    private String senderNickname; // 실명 또는 익명 닉네임 저장용

    @Column(name = "imageCount", nullable = false)
    private int imageCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "messageType")
    private MessageType messageType;

    @Column(name = "extraData", length = 2000)
    private String extraData;

    @Builder
    public ChatMessage(Long id, ChatRoom chatRoom, Member sender, String content, String senderNickname, int imageCount, MessageType messageType, String extraData, LocalDateTime createDate, LocalDateTime modifiedDate) {
        this.id = id;
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.senderNickname = senderNickname;
        this.imageCount = imageCount;
        this.messageType = messageType != null ? messageType : MessageType.TEXT;
        this.extraData = extraData;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
    }
}
