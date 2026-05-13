package kr.inuappcenterportal.inuportal.domain.chat.domain;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomStatus;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatRoomType;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_room")
public class ChatRoom extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    private boolean isAnonymous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomStatus status;

    @Column(nullable = false)
    private boolean isOfficial;

    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Member creator;

    @Builder
    public ChatRoom(String title, String description, int maxCapacity, boolean isAnonymous, ChatRoomType type, Member creator, boolean isOfficial, String thumbnailUrl) {
        this.title = title;
        this.description = description;
        this.maxCapacity = maxCapacity;
        this.isAnonymous = isAnonymous;
        this.type = type;
        this.status = ChatRoomStatus.ACTIVE;
        this.creator = creator;
        this.isOfficial = isOfficial;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void close() {
        this.status = ChatRoomStatus.CLOSED;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateInfo(String title, String description, String thumbnailUrl, int maxCapacity) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.maxCapacity = maxCapacity;
    }

    public void updateCreator(Member newCreator) {
        this.creator = newCreator;
    }
}
