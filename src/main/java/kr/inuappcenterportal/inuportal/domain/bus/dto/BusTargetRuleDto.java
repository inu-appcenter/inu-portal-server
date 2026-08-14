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

    @NotBlank(message = "시작 정류소 ID는 필수입니다.")
    @Schema(description = "시작 정류소 ID", example = "164000395")
    private String startBstopId;

    @NotBlank(message = "시작 정류장명은 필수입니다.")
    @Schema(description = "시작 정류장명", example = "인천대입구역 2번출구")
    private String startStopName;

    @Schema(description = "시작 정류장 축약명/별칭", example = "인입")
    private String startStopAlias;

    @Schema(description = "목표 도착 정류소 ID", example = "164000378")
    private String endBstopId;

    @Schema(description = "목표 도착 정류장명", example = "인천대학교 자연과학대학")
    private String endBstopName;

    @Schema(description = "도착 정류장 축약명/별칭", example = "자연대")
    private String endStopAlias;

    @Schema(description = "레거시 목적지 키워드 목록", example = "정문,자연,공과,공대,송도캠")
    private String targetKeywords;
}

