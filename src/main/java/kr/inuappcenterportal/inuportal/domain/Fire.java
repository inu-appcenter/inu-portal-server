package kr.inuappcenterportal.inuportal.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Fire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String duration;
    @Column
    private String averageDuration;
    @Column
    private Integer point;

    @Builder
    public Fire(String duration, String averageDuration){
        this.duration = duration;
        this.averageDuration = averageDuration;
        this.point = -1;
    }

    public void givePoint(int point){
        this.point = point;
    }
}