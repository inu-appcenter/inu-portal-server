package kr.inuappcenterportal.inuportal.domain.bus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "노선 구간 정보 직접 수정 요청 DTO")
public class RouteSectionUpdateRequest {

    @NotBlank(message = "구간명은 필수입니다.")
    @Schema(description = "구간명(별칭)", example = "인입런 - 8번 버스")
    private String sectionName;

    @NotBlank(message = "카테고리는 필수입니다.")
    @Schema(description = "카테고리 (go-school, go-home)", example = "go-school")
    private String category;

    @Schema(description = "탭명 (분류)", example = "인입런")
    private String tabName;

    @Schema(description = "운행시간 및 배차간격 안내 문구", example = "운행시간 | 05:54 ~ 00:31\n배차간격 | 5 ~ 13분")
    private String busNotice;

    @Schema(description = "한 줄 팁 또는 주의 코멘트", example = "많이 돌아가는 노선이니 주의하세요!")
    private String routeNotice;
}
