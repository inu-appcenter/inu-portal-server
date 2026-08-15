package kr.inuappcenterportal.inuportal.domain.bus.entity;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bus_service_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusServiceConfig extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(name = "description")
    private String description;

    @Builder
    public BusServiceConfig(String configKey, String configValue, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
    }

    public void updateValue(String newValue) {
        this.configValue = newValue;
    }

    public boolean isBooleanTrue() {
        return "true".equalsIgnoreCase(this.configValue) || "1".equals(this.configValue);
    }
}
