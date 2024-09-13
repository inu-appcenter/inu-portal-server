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
    @Schema(description = "미세먼지 농도",example = "5.3")
    private String pm10Value;
    @Schema(description = "미세먼지 등급",example = "좋음")
    private String pm10Grade;
    @Schema(description = "초미세먼지 농도",example = "5.3")
    private String pm25Value;
    @Schema(description = "초미세먼지 등급",example = "좋음")
    private String pm25Grade;
    @Schema(description = "낮과 밤의 상태",example = "낮")
    private String day;

    @Builder
    private WeatherResponseDto(String sky,String temperature,String pm10Value,String pm10Grade,String pm25Value,String pm25Grade, String day){
        this.sky = sky;
        this.temperature = temperature;
        this.pm10Value=pm10Value;
        this.pm10Grade=pm10Grade;
        this.pm25Value=pm25Value;
        this.pm25Grade=pm25Grade;
        this.day = day;
    }

    public static WeatherResponseDto of(String sky, String temperature,String pm10Value,String pm10Grade,String pm25Value,String pm25Grade, String day){
        return WeatherResponseDto.builder().sky(sky).temperature(temperature).pm10Value(pm10Value).pm10Grade(pm10Grade).pm25Value(pm25Value).pm25Grade(pm25Grade).day(day).build();
    }
}
