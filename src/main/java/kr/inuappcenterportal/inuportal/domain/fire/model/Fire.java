package kr.inuappcenterportal.inuportal.domain.fire.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fire")
public class Fire extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt")
    private String prompt;

    @Column(name = "request_id")
    private String requestId;


    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "is_rated")
    private Boolean isRated;

    @Column(name = "good_count")
    private Integer goodCount;

    @Builder
    public Fire(String prompt, String requestId, Long memberId) {
        this.prompt = prompt;
        this.requestId = requestId;
        this.memberId = memberId;
        this.isRated = false;
        this.goodCount = 0;
    }

    public void rate(){
        this.isRated = true;
    }

}
