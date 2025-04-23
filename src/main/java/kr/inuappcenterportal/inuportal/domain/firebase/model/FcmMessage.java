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

    @Builder
    public FcmMessage(String title, String body){
        this.title = title;
        this.body = body;
    }
}
