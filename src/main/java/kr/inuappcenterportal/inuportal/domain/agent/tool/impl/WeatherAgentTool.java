package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
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
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("WEATHER", "송도 캠퍼스 현재 날씨와 대기 상태를 조회합니다.",
                java.util.List.of("현재 하늘 상태·기온 조회", "미세먼지 등급 조회", "우산 필요 여부 판단에 필요한 현재 날씨 제공"),
                java.util.List.of("오늘 학교 날씨 어때?", "우산 필요해?", "미세먼지 어때?"),
                java.util.List.of("다른 지역 날씨", "장기 일기예보"), Map.of(), false, true);
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

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("날씨") || lower.contains("비") || lower.contains("우산") || lower.contains("기온") || lower.contains("미세먼지");
    }
}
