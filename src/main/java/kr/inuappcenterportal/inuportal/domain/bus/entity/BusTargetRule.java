package kr.inuappcenterportal.inuportal.domain.bus.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bus_target_rule")
public class BusTargetRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category; // go-school, go-home

    @Column(nullable = false)
    private String tabName; // 인입런, 지정단런, 인천대 정문, 공대/자연대 등

    @Column(nullable = false)
    private String startStopName; // 인천대입구역 2번출구, 인천대 공과대학 등

    @Column(nullable = false, length = 500)
    private String targetKeywords; // 콤마로 구분된 목적지 키워드 (예: "정문,자연,공과,공대,송도캠")

    @Builder
    public BusTargetRule(String category, String tabName, String startStopName, String targetKeywords) {
        this.category = category;
        this.tabName = tabName;
        this.startStopName = startStopName;
        this.targetKeywords = targetKeywords;
    }
}
