package kr.inuappcenterportal.inuportal.domain.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동적 렌더링 UI 컴포넌트 정보")
public record UiComponentDto(
        @Schema(description = "컴포넌트 유형 (WEATHER, CAFETERIA, BUS, TIMETABLE, SCHEDULE, NOTICE_LIST, DIRECTORY)", example = "NOTICE_LIST")
        String type,
        @Schema(description = "컴포넌트 바인딩 데이터 객체")
        Object data,
        @Schema(description = "딥링크 내비게이션 정보")
        UiComponentLinkDto link
) {
    public static UiComponentDto of(String type, Object data, String linkLabel, String route) {
        return new UiComponentDto(type, data, new UiComponentLinkDto(linkLabel, route));
    }

    public static UiComponentDto of(String type, Object data) {
        return new UiComponentDto(type, data, null);
    }
}
