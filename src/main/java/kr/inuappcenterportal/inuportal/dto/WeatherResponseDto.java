package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "날씨 응답 Dto")
@Getter
@NoArgsConstructor
public class WeatherResponseDto {
    @Schema(description = "하늘 상태",example = "맑음")
    private String sky;
    @Schema(description = "기온",example = "15.2")
    private String temperature;

    @Builder
    private WeatherResponseDto(String sky,String temperature){
        this.sky = sky;
        this.temperature = temperature;
    }

    public static WeatherResponseDto of(String sky, String temperature){
        return WeatherResponseDto.builder().sky(sky).temperature(temperature).build();
    }
}
