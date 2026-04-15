package kr.inuappcenterportal.inuportal.domain.featureflag.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "feature_flag",
        uniqueConstraints = @UniqueConstraint(name = "uk_feature_flag_key", columnNames = "flag_key")
)
public class FeatureFlag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_key", nullable = false, length = 100)
    private String flagKey;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean clientVisible;

    @Column(length = 255)
    private String description;

    @Builder
    private FeatureFlag(String flagKey, boolean enabled, boolean clientVisible, String description) {
        this.flagKey = flagKey;
        this.enabled = enabled;
        this.clientVisible = clientVisible;
        this.description = description;
    }

    public void update(boolean enabled, boolean clientVisible, String description) {
        this.enabled = enabled;
        this.clientVisible = clientVisible;
        this.description = description;
    }
}
