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

    @Schema(description = "승격 통합된 계정의 재발급 JWT 토큰")
    private TokenDto token;

    public MergeFreshmanResponseDto(Boolean merged, String examNo, TokenDto token) {
        this.merged = merged;
        this.examNo = examNo;
        this.token = token;
    }

    public static MergeFreshmanResponseDto notMerged() {
        return new MergeFreshmanResponseDto(false, null, null);
    }

    public static MergeFreshmanResponseDto merged(String examNo) {
        return new MergeFreshmanResponseDto(true, examNo, null);
    }

    public static MergeFreshmanResponseDto merged(String examNo, TokenDto token) {
        return new MergeFreshmanResponseDto(true, examNo, token);
    }
}
