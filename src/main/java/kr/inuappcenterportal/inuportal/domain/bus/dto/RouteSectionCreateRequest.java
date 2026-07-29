package kr.inuappcenterportal.inuportal.domain.bus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "어드민 동적 노선 생성/수정 요청 DTO")
public class RouteSectionCreateRequest {

    @NotBlank(message = "구간명은 필수입니다.")
    @Schema(description = "구간명", example = "인입런 - 8번 버스")
    private String sectionName;

    @NotBlank(message = "카테고리는 필수입니다.")
    @Schema(description = "카테고리 (go-school, go-home, shuttle)", example = "go-school")
    private String category;

    @NotBlank(message = "탭명은 필수입니다.")
    @Schema(description = "탭명", example = "인입런")
    private String tabName;

    @NotBlank(message = "노선 번호는 필수입니다.")
    @Schema(description = "노선 번호", example = "8")
    private String routeNo;

    @Schema(description = "기점 정류장 ID 또는 정류장명", example = "인천대입구역 2번출구")
    private String startStop;

    @Schema(description = "종점 정류장 ID 또는 정류장명", example = "인천대학교 자연과학대학")
    private String endStop;

    @Schema(description = "운행 시간 및 배차 간격 안내 문구", example = "운행시간 | 05:54 ~ 00:31\n배차간격 | 5 ~ 13분")
    private String busNotice;

    @Schema(description = "한 줄 팁 또는 경고 코멘트", example = "많이 돌아가는 노선이니 주의하세요!")
    private String routeNotice;
}

