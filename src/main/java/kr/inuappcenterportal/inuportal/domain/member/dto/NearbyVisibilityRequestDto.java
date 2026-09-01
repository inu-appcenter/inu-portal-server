package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "주변 친구 찾기 위치 노출 설정 요청 DTO")
public class NearbyVisibilityRequestDto {

    @NotNull(message = "설정 값이 필요합니다.")
    @Schema(description = "위치 노출 활성화 여부", example = "true")
    private Boolean enabled;
}
