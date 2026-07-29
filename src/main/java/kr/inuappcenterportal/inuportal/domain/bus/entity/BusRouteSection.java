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

    @Column(name = "bus_notice", length = 500)
    private String busNotice; // 운행시간 및 배차간격 안내 (예: "운행시간 | 05:54 ~ 00:31\n배차간격 | 5 ~ 13분")

    @Column(name = "route_notice", length = 500)
    private String routeNotice; // 한줄 팁/경고 (예: "많이 돌아가는 노선이니 주의하세요!")

    @OneToMany(mappedBy = "routeSection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<BusRouteStop> stops = new ArrayList<>();

    @Builder
    public BusRouteSection(String sectionName, String category, String tabName,
                           String routeNo, String routeId, String startBstopId,
                           String startBstopName, String endBstopId, String endBstopName,
                           String busNotice, String routeNotice) {
        this.sectionName = sectionName;
        this.category = category;
        this.tabName = tabName;
        this.routeNo = routeNo;
        this.routeId = routeId;
        this.startBstopId = startBstopId;
        this.startBstopName = startBstopName;
        this.endBstopId = endBstopId;
        this.endBstopName = endBstopName;
        this.busNotice = busNotice;
        this.routeNotice = routeNotice;
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
