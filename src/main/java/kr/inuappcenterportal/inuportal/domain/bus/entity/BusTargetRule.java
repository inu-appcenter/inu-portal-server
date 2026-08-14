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
    private String startBstopId; // 164000395 등 출발 정류소 ID

    @Column(nullable = false)
    private String startStopName; // 인천대입구역 2번출구 등 출발 정류장명

    @Column(nullable = true)
    private String startStopAlias; // 인입, 지정단 등 출발지 별칭

    @Column(nullable = true)
    private String endBstopId; // 164000378 등 목표 도착 정류소 ID

    @Column(nullable = true)
    private String endBstopName; // 인천대학교 자연과학대학 등 목표 도착 정류장명

    @Column(nullable = true)
    private String endStopAlias; // 자연대 등 도착지 별칭

    @Column(nullable = true, length = 500)
    private String targetKeywords; // 레거시 호환용

    @Builder
    public BusTargetRule(String category, String tabName, String startBstopId, String startStopName, String startStopAlias,
                         String endBstopId, String endBstopName, String endStopAlias, String targetKeywords) {
        this.category = category;
        this.tabName = tabName;
        this.startBstopId = startBstopId;
        this.startStopName = startStopName;
        this.startStopAlias = startStopAlias;
        this.endBstopId = endBstopId;
        this.endBstopName = endBstopName;
        this.endStopAlias = endStopAlias;
        this.targetKeywords = targetKeywords != null ? targetKeywords : "";
    }




}
