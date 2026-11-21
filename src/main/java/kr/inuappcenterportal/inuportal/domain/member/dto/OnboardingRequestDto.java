package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.member.enums.RealNameSource;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OnboardingRequestDto {

    @Schema(description = "설정할 닉네임", example = "횃불이123")
    @NotBlank(message = "닉네임을 입력해 주세요.")
    private String nickname;

    @Schema(description = "선택한 횃불이 캐릭터 ID", example = "1")
    @NotNull(message = "캐릭터를 선택해 주세요.")
    private Long fireId;

    @Schema(description = "실명 (공식 전산 또는 직접 입력)", example = "홍길동")
    private String realName;

    @Schema(description = "실명 출처 (PORTAL_SCRAPE 또는 MANUAL)", example = "PORTAL_SCRAPE")
    private RealNameSource realNameSource;

    @Schema(description = "소속 학과 코드", example = "CSE")
    private String departmentCode;

    @Schema(description = "공식 학적 인증 여부", example = "true")
    private Boolean isAcademicVerified;

    public OnboardingRequestDto(String nickname, Long fireId, String realName, RealNameSource realNameSource, String departmentCode, Boolean isAcademicVerified) {
        this.nickname = nickname;
        this.fireId = fireId;
        this.realName = realName;
        this.realNameSource = realNameSource;
        this.departmentCode = departmentCode;
        this.isAcademicVerified = isAcademicVerified != null && isAcademicVerified;
    }
}
