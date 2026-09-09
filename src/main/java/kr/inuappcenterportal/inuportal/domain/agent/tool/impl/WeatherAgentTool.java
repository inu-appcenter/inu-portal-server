package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.weather.dto.WeatherResponseDto;
import kr.inuappcenterportal.inuportal.domain.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherAgentTool implements AgentTool {

    private final WeatherService weatherService;

    @Override
    public String getName() {
        return "WEATHER";
    }

    @Override
    public String getDescription() {
        return "날씨, 기온, 미세먼지, 비, 우산 관련 질문 (params: 없음)";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        try {
            WeatherResponseDto weather = weatherService.getWeather();
            UiComponentDto component = UiComponentDto.of("WEATHER", weather, "송도 캠퍼스 날씨 홈", "/home");
            String summary = String.format("연수구 송도동은 현재 %s 상태이며 기온은 %s입니다. 미세먼지 등급은 %s입니다.",
                    weather.getSky(), weather.getTemperature(), weather.getPm10Grade());
            return new ToolResult(summary, component, weather);
        } catch (Exception e) {
            log.error("날씨 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("날씨 정보를 가져오는 데 실패했습니다.", null, null);
        }
    }
}
