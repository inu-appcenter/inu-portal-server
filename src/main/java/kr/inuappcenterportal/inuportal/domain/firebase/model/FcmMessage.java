package kr.inuappcenterportal.inuportal.domain.firebase.model;


import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fcm_message")
public class FcmMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String body;

    @Column(name = "is_admin_message", nullable = false)
    private boolean adminMessage = false;

    @Column(name = "send_count", nullable = false)
    private int sendCount = 0;

    @Builder
    public FcmMessage(String title, String body, boolean isAdminMessage) {
        this.title = title;
        this.body = body;
        this.adminMessage = isAdminMessage;
    }

    public void incrementSendCount(int count) {
        this.sendCount += count;
    }
}
