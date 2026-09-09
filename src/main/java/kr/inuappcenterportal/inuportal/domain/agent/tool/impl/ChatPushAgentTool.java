package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPushAgentTool implements AgentTool {

    private final MemberService memberService;

    @Override
    public String getName() {
        return "ACTION_CHAT_PUSH";
    }

    @Override
    public String getDescription() {
        return "채팅 푸시 알림 켜기/끄기 설정 (params: {\"enabled\": true|false}). 예: '채팅 알림 꺼줘' -> {\"enabled\": false}, '채팅 알림 켜줘' -> {\"enabled\": true}";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("채팅 알림 설정을 변경하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        try {
            boolean enabled = true;
            if (params != null && params.containsKey("enabled")) {
                Object val = params.get("enabled");
                if (val instanceof Boolean b) {
                    enabled = b;
                } else {
                    enabled = Boolean.parseBoolean(String.valueOf(val));
                }
            }
            boolean result = memberService.updateChatPush(member.getId(), enabled);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("settingType", "CHAT_PUSH");
            data.put("title", "채팅 푸시 알림");
            data.put("enabled", result);
            data.put("statusText", result ? "알림 켜짐" : "알림 꺼짐");
            data.put("message", result ? "채팅 푸시 알림이 활성화되었습니다." : "채팅 푸시 알림이 비활성화되었습니다.");

            UiComponentDto component = UiComponentDto.of("SETTING_RESULT", data, "내 정보 / 알림 설정", "/my-page");
            String summary = result 
                    ? "채팅 푸시 알림을 성공적으로 켰습니다. 새 메시지가 오면 푸시로 알려드릴게요!"
                    : "채팅 푸시 알림을 성공적으로 껐습니다. 언제든 다시 켜실 수 있어요.";

            return new ToolResult(summary, component, data);
        } catch (Exception e) {
            log.error("채팅 푸시 설정 변경 오류: {}", e.getMessage(), e);
            return new ToolResult("채팅 푸시 알림 설정을 변경하는 도중 오류가 발생했습니다.", null, null);
        }
    }
}
