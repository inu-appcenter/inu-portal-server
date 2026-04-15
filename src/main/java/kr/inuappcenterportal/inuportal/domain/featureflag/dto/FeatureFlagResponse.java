package kr.inuappcenterportal.inuportal.domain.featureflag.dto;

import kr.inuappcenterportal.inuportal.domain.featureflag.model.FeatureFlag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeatureFlagResponse {
    private String key;
    private boolean enabled;
    private boolean clientVisible;
    private String description;

    public static FeatureFlagResponse from(FeatureFlag flag) {
        return FeatureFlagResponse.builder()
                .key(flag.getFlagKey())
                .enabled(flag.isEnabled())
                .clientVisible(flag.isClientVisible())
                .description(flag.getDescription())
                .build();
    }
}
