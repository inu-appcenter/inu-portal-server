package kr.inuappcenterportal.inuportal.global.logging.domain;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "logging")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Logging extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private String memberId;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(nullable = false)
    private String uri;

    @Column(nullable = false)
    private long duration;

    private Logging(String memberId, String httpMethod, String uri, long duration) {
        this.memberId = memberId;
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.duration = duration;
    }

    public static Logging createLog(String memberId, String httpMethod, String uri, long duration) {
        return new Logging(memberId, httpMethod, uri, duration);
    }
}