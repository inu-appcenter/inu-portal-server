package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.member.enums.RealNameSource;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "회원 닉네임·횃불이·실명 변경 요청Dto. 보낸 칸만 바꾼다")
@Getter
@NoArgsConstructor
public class MemberUpdateNicknameDto {

    @Schema(description = "닉네임",example = "닉네임")
    private String nickname;

    @Schema(description = "횃불이 번호")
    @Min(1)
    @Max(12)
    private Long fireId;

    @Schema(description = "실명. 2~30자, 한글·영문·공백. 빈 문자열이면 삭제(동의 철회). 보내지 않으면 그대로", example = "최경민")
    private String realName;

    @Schema(description = "실명 출처. 실명을 넣을 때 필수. MANUAL / PORTAL_SCRAPE", example = "PORTAL_SCRAPE")
    private RealNameSource realNameSource;

    @Schema(description = "PORTAL_SCRAPE일 때 필수. 동의한 약관 버전", example = "2026-10-v1")
    private String consentTermsVersion;

    @Builder
    public MemberUpdateNicknameDto(String nickname, Long fireId){
        this.nickname = nickname;
        this.fireId = fireId;
    }
}
