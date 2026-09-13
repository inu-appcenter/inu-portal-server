package kr.inuappcenterportal.inuportal.domain.agent.tool;

import kr.inuappcenterportal.inuportal.domain.agent.tool.impl.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgentToolCatalogContractTest {

    private static final List<Class<? extends AgentTool>> STATIC_TOOLS = List.of(
            AcademicAgentTool.class, AgentReminderTool.class, BusAgentTool.class, CafeteriaAgentTool.class,
            CampusWatchAgentTool.class, ChatPushAgentTool.class, DailyBriefAgentTool.class,
            DirectoryAgentTool.class, InuAiAgentTool.class, LibraryAgentTool.class, LmsAgentTool.class,
            MySettingsAgentTool.class, NoticeAgentTool.class, NoticeKeywordAgentTool.class,
            ScheduleAgentTool.class, TimeTableAgentTool.class, TimeTableGapAgentTool.class, WeatherAgentTool.class
    );

    @Test
    void everyStaticToolHasACompleteUniqueDefinition() throws Exception {
        Set<String> names = new HashSet<>();

        for (Class<? extends AgentTool> type : STATIC_TOOLS) {
            AgentToolDefinition definition = instantiate(type).getDefinition();
            assertFalse(definition.summary().isBlank(), type.getSimpleName());
            assertFalse(definition.capabilities().isEmpty(), type.getSimpleName());
            assertFalse(definition.triggerExamples().isEmpty(), type.getSimpleName());
            assertTrue(names.add(definition.name()), "중복 도구명: " + definition.name());
        }
    }

    @Test
    void catalogDoesNotAdvertiseUnsupportedLibraryOrStudyRoomActions() throws Exception {
        AgentToolDefinition library = instantiate(LibraryAgentTool.class).getDefinition();
        AgentToolDefinition watch = instantiate(CampusWatchAgentTool.class).getDefinition();

        assertEquals(List.of("SEATS", "STUDY_ROOMS", "MY_SEAT", "RENEW_SEAT", "RETURN_SEAT", "RESERVE_STUDY_ROOM", "CANCEL_STUDY_ROOM", "CHECKIN_STUDY_ROOM"),
                library.parameters().get("target").enumValues());
        assertEquals(List.of("LIBRARY_SEAT", "STUDY_ROOM"), watch.parameters().get("domain").enumValues());
        assertTrue(library.negativeExamples().stream().anyMatch(text -> text.contains("ACTION_CAMPUS_WATCH")));
        assertTrue(watch.negativeExamples().stream().anyMatch(text -> text.contains("LIBRARY")));
    }

    @Test
    void representativeUtterancesAreCoveredByRuleBasedFallbacks() throws Exception {
        Map<Class<? extends AgentTool>, String> utterances = Map.ofEntries(
                Map.entry(AcademicAgentTool.class, "내 지도교수님 누구야?"),
                Map.entry(AgentReminderTool.class, "매일 11시에 학식 알려줘"),
                Map.entry(BusAgentTool.class, "정문 버스 언제 와?"),
                Map.entry(CafeteriaAgentTool.class, "오늘 점심 메뉴 알려줘"),
                Map.entry(CampusWatchAgentTool.class, "제1열람실 자리 나면 알려줘"),
                Map.entry(ChatPushAgentTool.class, "채팅 알림 꺼줘"),
                Map.entry(DailyBriefAgentTool.class, "데일리 브리프 켜줘"),
                Map.entry(DirectoryAgentTool.class, "컴퓨터공학부 사무실 전화번호 알려줘"),
                Map.entry(InuAiAgentTool.class, "휴학 규정 알려줘"),
                Map.entry(LibraryAgentTool.class, "도서관 자리 있어?"),
                Map.entry(LmsAgentTool.class, "미제출 과제 있어?"),
                Map.entry(MySettingsAgentTool.class, "내 알림 설정 보여줘"),
                Map.entry(NoticeAgentTool.class, "장학금 공지 찾아줘"),
                Map.entry(NoticeKeywordAgentTool.class, "장학금 공지 올라오면 알려줘"),
                Map.entry(ScheduleAgentTool.class, "이번 달 학사일정 알려줘"),
                Map.entry(TimeTableAgentTool.class, "오늘 수업 뭐 있어?"),
                Map.entry(TimeTableGapAgentTool.class, "오늘 공강 언제야?"),
                Map.entry(WeatherAgentTool.class, "오늘 학교 날씨 어때?")
        );

        for (Map.Entry<Class<? extends AgentTool>, String> entry : utterances.entrySet()) {
            assertTrue(instantiate(entry.getKey()).supportsFallback(entry.getValue(), List.of()),
                    () -> entry.getKey().getSimpleName() + " fallback 누락: " + entry.getValue());
        }
    }

    private <T extends AgentTool> T instantiate(Class<T> type) throws Exception {
        Constructor<?> constructor = java.util.Arrays.stream(type.getDeclaredConstructors())
                .max(java.util.Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();
        constructor.setAccessible(true);
        Object[] arguments = java.util.Arrays.stream(constructor.getParameterTypes())
                .map(parameterType -> mock(parameterType))
                .toArray();
        return type.cast(constructor.newInstance(arguments));
    }
}
