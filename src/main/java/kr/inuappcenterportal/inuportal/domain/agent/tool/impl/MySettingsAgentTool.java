package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.res.DailyBriefSettingResponseDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.service.DailyBriefService;
import kr.inuappcenterportal.inuportal.domain.keyword.dto.res.KeywordResponse;
import kr.inuappcenterportal.inuportal.domain.keyword.service.KeywordService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MySettingsAgentTool implements AgentTool {

    private final MemberService memberService;
    private final DailyBriefService dailyBriefService;
    private final KeywordService keywordService;

    @Override
    public String getName() {
        return "ACTION_MY_SETTINGS";
    }

    @Override
    public String getDescription() {
        return "내 알림 설정 현황 및 키워드 목록 조회 (params: 없음). 예: '내 알림 설정 보여줘', '내가 등록한 키워드 뭐 있어?'";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("내 알림 설정을 확인하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        try {
            boolean chatPush = Boolean.TRUE.equals(member.getChatPushEnabled());
            DailyBriefSettingResponseDto brief = dailyBriefService.getSettings(member);
            List<KeywordResponse> keywords = keywordService.getKeywords(member);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("chatPushEnabled", chatPush);
            data.put("dailyBrief", brief);
            data.put("keywordCount", keywords.size());
            data.put("keywords", keywords);

            UiComponentDto component = UiComponentDto.of("MY_SETTINGS", data, "설정 페이지 가기", "/mypage");

            StringBuilder sb = new StringBuilder();
            sb.append("현재 회원님의 알림 및 설정 현황입니다:\n");
            sb.append(String.format("• 채팅 푸시 알림: %s\n", chatPush ? "켜짐 🔔" : "꺼짐 🔕"));
            sb.append(String.format("• 데일리 브리프: 시간표(%s, %s) / 학사일정(%s, %s)\n",
                    Boolean.TRUE.equals(brief.timetableDailyBriefEnabled()) ? "켜짐" : "꺼짐",
                    brief.timetableDailyBriefTime() != null ? brief.timetableDailyBriefTime() : "08:00",
                    Boolean.TRUE.equals(brief.scheduleAlertEnabled()) ? "켜짐" : "꺼짐",
                    brief.scheduleDailyBriefTime() != null ? brief.scheduleDailyBriefTime() : "08:30"));
            sb.append(String.format("• 등록된 공지 알림 키워드: 총 %d개", keywords.size()));
            if (!keywords.isEmpty()) {
                sb.append(" (");
                for (int i = 0; i < Math.min(keywords.size(), 3); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(keywords.get(i).keyword());
                }
                if (keywords.size() > 3) sb.append(String.format(" 외 %d개", keywords.size() - 3));
                sb.append(")");
            }

            return new ToolResult(sb.toString().trim(), component, data);
        } catch (Exception e) {
            log.error("알림 설정 조회 오류: {}", e.getMessage(), e);
            return new ToolResult("알림 설정을 조회하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("알림 설정") || lower.contains("내 설정") || lower.contains("내 알림");
    }
}
