package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 위치 정보 갱신 요청 DTO")
public class LocationUpdateRequestDto {

    @Schema(description = "위도 (-90.0 ~ 90.0)", example = "37.4638")
    private Double latitude;

    @Schema(description = "경도 (-180.0 ~ 180.0)", example = "126.6321")
    private Double longitude;
}
