package kr.inuappcenterportal.inuportal.domain.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentReminderDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentReminderUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.agent.enums.AgentReminderRepeatType;
import kr.inuappcenterportal.inuportal.domain.agent.model.AgentReminder;
import kr.inuappcenterportal.inuportal.domain.agent.repository.AgentReminderRepository;
import kr.inuappcenterportal.inuportal.domain.agent.service.AgentReminderService;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolRegistry;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentReminderServiceTest {

    @Mock
    private AgentReminderRepository agentReminderRepository;

    @Mock
    private AgentToolRegistry agentToolRegistry;

    @Mock
    private FcmService fcmService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AgentReminderService agentReminderService;

    private Member createTestMember(Long id) {
        Member member = Member.builder()
                .studentId("202100001")
                .roles(List.of("ROLE_USER"))
                .build();
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
        } catch (Exception ignored) {}
        return member;
    }

    @Test
    @DisplayName("맞춤 알림 등록 성공 테스트")
    void createReminder_success() {
        // given
        Member member = createTestMember(1L);
        AgentTool mockTool = mock(AgentTool.class);
        given(agentToolRegistry.findTool("CAFETERIA")).willReturn(Optional.of(mockTool));

        AgentReminder saved = AgentReminder.builder()
                .member(member)
                .title("점심 학식 알림")
                .targetTime("11:00")
                .repeatType(AgentReminderRepeatType.WEEKDAYS)
                .targetTool("CAFETERIA")
                .toolParamsJson("{\"mealType\":\"LUNCH\"}")
                .titleTemplate("🍱 오늘의 학식")
                .bodyTemplate("오늘 메뉴: {mainMenu}")
                .route("/cafeteria")
                .enabled(true)
                .build();

        given(agentReminderRepository.save(any(AgentReminder.class))).willReturn(saved);

        // when
        AgentReminderDto dto = agentReminderService.createReminder(
                member,
                "점심 학식 알림",
                "11:00",
                AgentReminderRepeatType.WEEKDAYS,
                "CAFETERIA",
                "{\"mealType\":\"LUNCH\"}",
                "🍱 오늘의 학식",
                "오늘 메뉴: {mainMenu}",
                "/cafeteria"
        );

        // then
        assertThat(dto.title()).isEqualTo("점심 학식 알림");
        assertThat(dto.targetTime()).isEqualTo("11:00");
        assertThat(dto.targetTool()).isEqualTo("CAFETERIA");
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("정직성 가드레일: 지원하지 않는 도구 등록 시 예외 발생")
    void createReminder_invalidTool() {
        // given
        Member member = createTestMember(1L);
        given(agentToolRegistry.findTool("INVALID_TOOL")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> agentReminderService.createReminder(
                member,
                "이상한 알림",
                "11:00",
                AgentReminderRepeatType.WEEKDAYS,
                "INVALID_TOOL",
                "{}",
                "",
                "",
                ""
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 기능 도구");
    }

    @Test
    @DisplayName("맞춤 알림 수정 및 On/Off 토글 테스트")
    void updateAndToggleReminder() {
        // given
        Member member = createTestMember(1L);
        AgentReminder reminder = AgentReminder.builder()
                .member(member)
                .title("학식 알림")
                .targetTime("11:00")
                .repeatType(AgentReminderRepeatType.WEEKDAYS)
                .targetTool("CAFETERIA")
                .enabled(true)
                .build();

        given(agentReminderRepository.findByIdAndMemberId(10L, 1L)).willReturn(Optional.of(reminder));

        // when 1: 시간 수정
        AgentReminderUpdateRequestDto req = new AgentReminderUpdateRequestDto(
                "수정된 학식 알림",
                "11:30",
                AgentReminderRepeatType.EVERYDAY,
                null, null, null, null, null
        );
        AgentReminderDto updated = agentReminderService.updateReminder(10L, member, req);

        // then 1
        assertThat(updated.title()).isEqualTo("수정된 학식 알림");
        assertThat(updated.targetTime()).isEqualTo("11:30");
        assertThat(updated.repeatType()).isEqualTo(AgentReminderRepeatType.EVERYDAY);

        // when 2: On/Off 토글
        AgentReminderDto toggled = agentReminderService.toggleReminder(10L, member, false);

        // then 2
        assertThat(toggled.enabled()).isFalse();
    }

    @Test
    @DisplayName("스케줄러 디스패치: Zero-LLM 템플릿 치환 및 FCM 발송 검증")
    void dispatchDueReminders_success() {
        // given
        Member member = createTestMember(1L);
        String currentTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        AgentReminder reminder = AgentReminder.builder()
                .member(member)
                .title("점심 학식 알림")
                .targetTime(currentTimeStr)
                .repeatType(AgentReminderRepeatType.EVERYDAY)
                .targetTool("CAFETERIA")
                .toolParamsJson("{\"restaurant\":\"DORMITORY_1\"}")
                .titleTemplate("🍱 오늘의 11시 학식 배달")
                .bodyTemplate("제1기숙사: {mainMenu} ({price}원)")
                .route("/cafeteria")
                .enabled(true)
                .build();

        given(agentReminderRepository.findAllActiveByTargetTime(currentTimeStr))
                .willReturn(List.of(reminder));

        Map<String, Object> liveData = Map.of(
                "mainMenu", "수제 치즈돈까스",
                "price", "5,500"
        );
        AgentTool.ToolResult mockResult = AgentTool.ToolResult.of("돈까스 요약", null, liveData);
        given(agentToolRegistry.execute(eq("CAFETERIA"), eq(member), anyMap()))
                .willReturn(mockResult);

        // when
        agentReminderService.dispatchDueReminders();

        // then
        verify(fcmService, times(1)).sendDailyBriefNotification(
                eq(1L),
                eq("🍱 오늘의 11시 학식 배달"),
                eq("제1기숙사: 수제 치즈돈까스 (5,500원)"),
                eq(FcmMessageType.AGENT_CUSTOM_REMINDER),
                eq("/cafeteria")
        );
        assertThat(reminder.getLastSentDate()).isEqualTo(LocalDate.now());
    }
}
