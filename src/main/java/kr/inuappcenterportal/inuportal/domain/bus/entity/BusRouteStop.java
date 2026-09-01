package kr.inuappcenterportal.inuportal.domain.bus.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bus_route_stop")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusRouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_section_id")
    private BusRouteSection routeSection;

    @Column(name = "seq", nullable = false)
    private Integer seq; // 정류장 순번

    @Column(name = "bstop_id", nullable = false)
    private String bstopId;

    @Column(name = "bstop_name", nullable = false)
    private String bstopName;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Builder
    public BusRouteStop(Integer seq, String bstopId, String bstopName, Double latitude, Double longitude) {
        this.seq = seq;
        this.bstopId = bstopId;
        this.bstopName = bstopName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void assignSection(BusRouteSection routeSection) {
        this.routeSection = routeSection;
    }
}
