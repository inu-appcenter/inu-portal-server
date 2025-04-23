package kr.inuappcenterportal.inuportal.domain.firebase.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fcm_token")
public class FcmToken{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "member_id")
    private Long memberId;
    @Column(name = "token",length = 512,unique = true,nullable = false)
    private String token;
    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Builder
    public FcmToken(Long memberId, String token) {
        this.memberId = memberId;
        this.token = token;
        this.createDate = LocalDateTime.now();
    }

    public void updateTimeNow(){
        this.createDate = LocalDateTime.now();
    }
    public void clearMemberId(){
        this.memberId = null;
    }

}
