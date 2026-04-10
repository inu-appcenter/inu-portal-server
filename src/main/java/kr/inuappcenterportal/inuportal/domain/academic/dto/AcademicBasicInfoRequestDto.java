package kr.inuappcenterportal.inuportal.domain.academic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "포털 학적 정보 조회 요청")
public class AcademicBasicInfoRequestDto {

    @NotBlank
    @Schema(description = "인천대학교 포털 아이디", example = "202001518")
    private String portalId;

    @NotBlank
    @Schema(description = "인천대학교 포털 비밀번호", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String portalPassword;

    @Builder
    public AcademicBasicInfoRequestDto(String portalId, String portalPassword) {
        this.portalId = portalId;
        this.portalPassword = portalPassword;
    }
}
