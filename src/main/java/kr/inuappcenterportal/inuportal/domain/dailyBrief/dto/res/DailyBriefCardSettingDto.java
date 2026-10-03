package kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Daily Brief 카드 설정 DTO")
public record DailyBriefCardSettingDto(
        @Schema(description = "카드 구성 및 세부 설정 JSON 문자열", example = "{\"mode\":\"auto\"}")
        String cardSettingsJson
) {
    public static DailyBriefCardSettingDto of(String cardSettingsJson) {
        return new DailyBriefCardSettingDto(cardSettingsJson);
    }
}
