package kr.inuappcenterportal.inuportal.domain.bus.entity;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bus_target_stop")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusTargetStop extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bstop_id", nullable = false, unique = true)
    private String bstopId;

    @Column(name = "bstop_name", nullable = false)
    private String bstopName;

    @Column(name = "stop_alias")
    private String stopAlias; // 축약명/별칭 (예: "인입", "지정단", "정문", "공대")

    @Column(name = "category")
    private String category; // 예: "인입런", "인천대 정문", "기숙사 앞"

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    public BusTargetStop(String bstopId, String bstopName, String stopAlias, String category, Boolean isActive) {
        this.bstopId = bstopId;
        this.bstopName = bstopName;
        this.stopAlias = stopAlias;
        this.category = category;
        if (isActive != null) {
            this.isActive = isActive;
        }
    }


    public void updateActive(boolean isActive) {
        this.isActive = isActive;
    }
}
