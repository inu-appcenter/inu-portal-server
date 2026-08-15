package kr.inuappcenterportal.inuportal.domain.bus.entity;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bus_stop_alias")
public class BusStopAlias extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bstop_id", nullable = false, unique = true)
    private String bstopId; // 공공데이터 정류소 ID (예: 164000395)

    @Column(name = "bstop_name", nullable = false)
    private String bstopName; // 공식 정류소 명칭 (예: 인천대입구역 2번출구)

    @Column(name = "stop_alias", nullable = false)
    private String stopAlias; // 축약명/별칭 (예: 인입, 지정단, 정문, 공대, 자연대)

    @Column(name = "stop_notice", length = 500)
    private String stopNotice; // 정류장 상단 안내 문구 (예: "※ 8시 ~ 10시에는 매우 혼잡해요...")

    @Column(name = "memo")
    private String memo; // 관리자 메모

    @Builder
    public BusStopAlias(String bstopId, String bstopName, String stopAlias, String stopNotice, String memo) {
        this.bstopId = bstopId;
        this.bstopName = bstopName;
        this.stopAlias = stopAlias;
        this.stopNotice = stopNotice;
        this.memo = memo;
    }

    public void update(String bstopName, String stopAlias, String stopNotice, String memo) {
        if (bstopName != null) this.bstopName = bstopName;
        if (stopAlias != null) this.stopAlias = stopAlias;
        if (stopNotice != null) this.stopNotice = stopNotice;
        if (memo != null) this.memo = memo;
    }
}

