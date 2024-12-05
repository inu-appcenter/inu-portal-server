package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "신고 등록 Dto")
public class ReportRequestDto {
    @Schema(description = "신고 사유",example = "잘못된 정보")
    @NotBlank
    private String reason;
    @Schema(description = "신고 코멘트",example = "7호관은 도서관이 아닙니다.")
    private String comment;
}
