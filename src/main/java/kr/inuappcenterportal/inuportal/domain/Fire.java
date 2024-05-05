package kr.inuappcenterportal.inuportal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fire")
public class Fire extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String param;

    @Column
    private String duration;
    @Column(name = "average_duration")
    private String averageDuration;
    @Column
    private Integer point;
    @Column(name = "is_rated")
    private Boolean isRated;

    @Builder
    public Fire(String duration, String averageDuration, String param){
        this.duration = duration;
        this.averageDuration = averageDuration;
        this.point = -1;
        this.isRated = false;
        this.param = param;
    }

    public void givePoint(int point){
        this.point = point;
        this.isRated = true;
    }
}
