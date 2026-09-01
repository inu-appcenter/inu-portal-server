package kr.inuappcenterportal.inuportal.domain.suggestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "건의사항 처리 상태 변경 요청 Dto (관리자 전용)")
@Getter
@NoArgsConstructor
public class SuggestionStatusRequest {

    @Schema(description = "변경할 처리 상태 (개발/운영팀의 내부 진행 상황이며, 사용자에게 공개되는 상담 진행 상태가 아닙니다)", example = "COMPLETED", allowableValues = {"RECEIVED", "IN_REVIEW", "PLANNED", "COMPLETED", "ON_HOLD"})
    @NotBlank
    private String status;

    @Builder
    public SuggestionStatusRequest(String status) {
        this.status = status;
    }
}
