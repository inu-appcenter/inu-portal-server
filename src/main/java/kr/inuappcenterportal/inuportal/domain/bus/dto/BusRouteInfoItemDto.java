package kr.inuappcenterportal.inuportal.domain.bus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "노선 기본 정보 DTO")
public class BusRouteInfoItemDto {

    @Schema(description = "노선 ID", example = "165000012")
    private String routeId;

    @Schema(description = "노선 번호", example = "8")
    private String routeNo;
}
