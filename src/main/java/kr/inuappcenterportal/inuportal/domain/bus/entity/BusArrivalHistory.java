package kr.inuappcenterportal.inuportal.domain.bus.entity;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bus_arrival_history", indexes = {
        @Index(name = "idx_bstop_date", columnList = "bstop_id, create_date"),
        @Index(name = "idx_route_bstop_date", columnList = "route_id, bstop_id, create_date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusArrivalHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bstop_id", nullable = false)
    private String bstopId;

    @Column(name = "bstop_name")
    private String bstopName;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "route_no")
    private String routeNo;

    @Column(name = "bus_id")
    private String busId;

    @Column(name = "bus_num_plate")
    private String busNumPlate;

    @Column(name = "arrival_estimate_time")
    private Integer arrivalEstimateTime;

    @Column(name = "rest_stop_count")
    private Integer restStopCount;

    @Column(name = "congestion")
    private String congestion;

    @Builder
    public BusArrivalHistory(String bstopId, String bstopName, String routeId, String routeNo,
                             String busId, String busNumPlate, Integer arrivalEstimateTime,
                             Integer restStopCount, String congestion) {
        this.bstopId = bstopId;
        this.bstopName = bstopName;
        this.routeId = routeId;
        this.routeNo = routeNo;
        this.busId = busId;
        this.busNumPlate = busNumPlate;
        this.arrivalEstimateTime = arrivalEstimateTime;
        this.restStopCount = restStopCount;
        this.congestion = congestion;
    }
}
