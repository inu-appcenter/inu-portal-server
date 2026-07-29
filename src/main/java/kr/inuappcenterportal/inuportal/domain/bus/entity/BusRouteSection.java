package kr.inuappcenterportal.inuportal.domain.bus.entity;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bus_route_section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusRouteSection extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_name", nullable = false)
    private String sectionName; // 예: "인입런 - 8번 버스"

    @Column(name = "category", nullable = false)
    private String category; // 예: "go-school", "go-home"

    @Column(name = "tab_name")
    private String tabName; // 예: "인입런", "지정단런", "인천대 정문"

    @Column(name = "route_no", nullable = false)
    private String routeNo; // 예: "8"

    @Column(name = "route_id")
    private String routeId; // 공공데이터포털 노선 ID

    @Column(name = "start_bstop_id")
    private String startBstopId;

    @Column(name = "start_bstop_name")
    private String startBstopName;

    @Column(name = "end_bstop_id")
    private String endBstopId;

    @Column(name = "end_bstop_name")
    private String endBstopName;

    @OneToMany(mappedBy = "routeSection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<BusRouteStop> stops = new ArrayList<>();

    @Builder
    public BusRouteSection(String sectionName, String category, String tabName,
                           String routeNo, String routeId, String startBstopId,
                           String startBstopName, String endBstopId, String endBstopName) {
        this.sectionName = sectionName;
        this.category = category;
        this.tabName = tabName;
        this.routeNo = routeNo;
        this.routeId = routeId;
        this.startBstopId = startBstopId;
        this.startBstopName = startBstopName;
        this.endBstopId = endBstopId;
        this.endBstopName = endBstopName;
    }

    public void updateStops(List<BusRouteStop> newStops) {
        this.stops.clear();
        if (newStops != null) {
            newStops.forEach(stop -> {
                stop.assignSection(this);
                this.stops.add(stop);
            });
        }
    }
}
