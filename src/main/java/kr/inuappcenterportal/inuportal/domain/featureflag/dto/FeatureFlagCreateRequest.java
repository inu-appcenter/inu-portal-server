package kr.inuappcenterportal.inuportal.domain.featureflag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FeatureFlagCreateRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_]{2,100}$")
    private String key;

    @NotNull
    private Boolean enabled;

    @NotNull
    private Boolean clientVisible;

    private String description;
}
