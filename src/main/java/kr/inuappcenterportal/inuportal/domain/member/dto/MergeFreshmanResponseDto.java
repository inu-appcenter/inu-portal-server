package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MergeFreshmanResponseDto {

    @Schema(description = "통합 성공 여부", example = "true")
    private Boolean merged;

    @Schema(description = "통합된 신입생 수험번호", example = "202612345")
    private String examNo;

    public MergeFreshmanResponseDto(Boolean merged, String examNo) {
        this.merged = merged;
        this.examNo = examNo;
    }

    public static MergeFreshmanResponseDto notMerged() {
        return new MergeFreshmanResponseDto(false, null);
    }

    public static MergeFreshmanResponseDto merged(String examNo) {
        return new MergeFreshmanResponseDto(true, examNo);
    }
}
