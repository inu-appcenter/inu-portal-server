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
    @Column(name = "device_type")
    private String deviceType;
    @Column(name = "create_date")
    private LocalDateTime createDate;
    /**
     * iOS ActivityKit push-to-start 토큰. 같은 기기(FCM 토큰)의 시간표 Live Activity를
     * 앱이 꺼져 있어도 서버 푸시로 시작하기 위해 쓴다. iOS 17.2+ 기기만 가지며, 앱에서
     * 기능을 끄면 null로 지워진다.
     */
    @Column(name = "live_activity_start_token", length = 512)
    private String liveActivityStartToken;

    @Builder
    public FcmToken(Long memberId, String token, String deviceType) {
        this.memberId = memberId;
        this.token = token;
        this.deviceType = deviceType;
        this.createDate = LocalDateTime.now();
    }

    public void updateMemberId(Long memberId){
        this.memberId = memberId;
    }
    public void updateTimeNow(){
        this.createDate = LocalDateTime.now();
    }
    public void clearMemberId(){
        this.memberId = null;
    }
    public void updateDeviceType(String deviceType) {
        if (deviceType == null || deviceType.isBlank()) {
            return;
        }
        this.deviceType = deviceType;
    }
    public void updateLiveActivityStartToken(String liveActivityStartToken) {
        this.liveActivityStartToken = (liveActivityStartToken == null || liveActivityStartToken.isBlank())
                ? null
                : liveActivityStartToken;
    }

}
