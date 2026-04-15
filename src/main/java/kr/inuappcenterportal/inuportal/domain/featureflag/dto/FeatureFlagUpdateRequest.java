package kr.inuappcenterportal.inuportal.domain.featureflag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FeatureFlagUpdateRequest {

    @NotNull
    private Boolean enabled;

    @NotNull
    private Boolean clientVisible;

    private String description;
}

