package kr.inuappcenterportal.inuportal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fire")
public class Fire extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt")
    private String prompt;

    @Column(name = "request_id")
    private String requestId;


    @Column(name = "member_id")
    private Long memberId;


    @Builder
    public Fire(String prompt, String requestId, Long memberId) {
        this.prompt = prompt;
        this.requestId = requestId;
        this.memberId = memberId;
    }

}
