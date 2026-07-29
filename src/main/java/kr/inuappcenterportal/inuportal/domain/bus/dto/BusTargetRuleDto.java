package kr.inuappcenterportal.inuportal.domain.bus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "자동 탐색 타겟 규칙 DTO")
public class BusTargetRuleDto {

    private Long id;

    @NotBlank(message = "카테고리는 필수입니다.")
    @Schema(description = "카테고리 (go-school, go-home)", example = "go-school")
    private String category;

    @NotBlank(message = "탭명은 필수입니다.")
    @Schema(description = "탭명", example = "인입런")
    private String tabName;

    @NotBlank(message = "시작 정류장명은 필수입니다.")
    @Schema(description = "시작 정류장명", example = "인천대입구역 2번출구")
    private String startStopName;

    @NotBlank(message = "목적지 키워드는 필수입니다.")
    @Schema(description = "콤마로 구분된 목적지 키워드 목록", example = "정문,자연,공과,공대,송도캠")
    private String targetKeywords;
}
