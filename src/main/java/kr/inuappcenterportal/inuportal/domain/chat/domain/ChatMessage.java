package kr.inuappcenterportal.inuportal.domain.chat.domain;

import jakarta.persistence.*;
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

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "senderNickname", nullable = false)
    private String senderNickname; // 실명 또는 익명 닉네임 저장용

    @Column(name = "imageCount", nullable = false)
    private int imageCount;

    @Builder
    public ChatMessage(Long id, ChatRoom chatRoom, Member sender, String content, String senderNickname, int imageCount, LocalDateTime createDate, LocalDateTime modifiedDate) {
        this.id = id;
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.senderNickname = senderNickname;
        this.imageCount = imageCount;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
    }
}
