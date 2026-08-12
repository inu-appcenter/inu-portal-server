package kr.inuappcenterportal.inuportal.domain.suggestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "건의사항 답변/상태변경 요청 Dto (관리자 전용)")
@Getter
@NoArgsConstructor
public class SuggestionAnswerRequest {

    @Schema(description = "변경할 처리 상태", example = "COMPLETED", allowableValues = {"RECEIVED", "IN_REVIEW", "PLANNED", "COMPLETED", "ON_HOLD"})
    @NotBlank
    private String status;

    @Schema(description = "운영진 답변 내용 (답변 없이 상태만 변경할 경우 생략 가능)", example = "다음 업데이트에 반영 예정입니다.")
    @Size(max = 2000)
    private String answerContent;

    @Builder
    public SuggestionAnswerRequest(String status, String answerContent) {
        this.status = status;
        this.answerContent = answerContent;
    }
}
