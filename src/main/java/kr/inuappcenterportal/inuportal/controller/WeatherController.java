package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.dto.WeatherResponseDto;
import kr.inuappcenterportal.inuportal.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@Tag(name="Weathers", description = "날씨 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/weathers")
public class WeatherController {
    private final WeatherService weatherService;

    @Operation(summary = "날씨 가져오기",description = "날씨의 종류에는 맑음, 구름, 비, 눈, 진눈깨비가 있습니다. 미세먼지의 상태에는 좋음, 보통, 나쁨, 매우나쁨이 있습니다.낮,밤의 경우 day,night로 구분됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "날씨 가져오기 성공",content = @Content(schema = @Schema(implementation = WeatherResponseDto.class))),
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<WeatherResponseDto>> getWeather(){
        return ResponseEntity.ok(ResponseDto.of(weatherService.getWeather(),"날씨 가져오기 성공"));
    }
}
