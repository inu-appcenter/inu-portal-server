package kr.inuappcenterportal.inuportal.domain.chat.domain;

import jakarta.persistence.*;
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

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    private boolean isAnonymous;

    @Builder
    public ChatRoom(String title, int maxCapacity, boolean isAnonymous) {
        this.title = title;
        this.maxCapacity = maxCapacity;
        this.isAnonymous = isAnonymous;
    }
}
