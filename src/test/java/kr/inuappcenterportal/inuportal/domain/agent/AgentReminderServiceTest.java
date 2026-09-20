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
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableService;
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

    @Mock
    private TimeTableService timeTableService;

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
                null, null, null, null, null, null, null
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
    @DisplayName("다중 스케줄 등록 및 스케줄러 디스패치 검증")
    void dispatchDueReminders_multiSchedule_success() {
        // given
        Member member = createTestMember(1L);
        LocalDate today = LocalDate.now();
        String day3 = today.getDayOfWeek().name().substring(0, 3);
        String currentTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        String schedulesJson = String.format("[{\"days\":[\"%s\"],\"time\":\"%s\",\"repeatType\":\"WEEKDAYS\"}]", day3, currentTimeStr);

        AgentReminder reminder = AgentReminder.builder()
                .member(member)
                .title("다중 스케줄 알림")
                .targetTime(currentTimeStr)
                .repeatType(AgentReminderRepeatType.WEEKDAYS)
                .targetTool("CAFETERIA")
                .schedulesJson(schedulesJson)
                .toolParamsJson("{\"restaurant\":\"DORMITORY_1\"}")
                .titleTemplate("🍱 11시 학식 알림")
                .bodyTemplate("학식: {mainMenu}")
                .route("/cafeteria")
                .enabled(true)
                .build();

        given(agentReminderRepository.findAllActive()).willReturn(List.of(reminder));

        Map<String, Object> liveData = Map.of("mainMenu", "제육덮밥");
        AgentTool.ToolResult mockResult = AgentTool.ToolResult.of("제육 요약", null, liveData);
        given(agentToolRegistry.execute(eq("CAFETERIA"), eq(member), anyMap()))
                .willReturn(mockResult);

        // when
        agentReminderService.dispatchDueReminders();

        // then
        verify(fcmService, times(1)).sendDailyBriefNotification(
                eq(1L),
                eq("🍱 11시 학식 알림"),
                eq("학식: 제육덮밥"),
                eq(FcmMessageType.AGENT_CUSTOM_REMINDER),
                eq("/cafeteria")
        );
        assertThat(reminder.getLastSentDate()).isEqualTo(today);
        assertThat(reminder.getLastSentTime()).isEqualTo(currentTimeStr);
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

        given(agentReminderRepository.findAllActive())
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

    @Test
    @DisplayName("임의 도구/기본 루틴 즉시 테스트 발송(testDispatchCustom) 검증")
    void testDispatchCustom_success() {
        // given
        Member member = createTestMember(1L);
        AgentTool mockTool = mock(AgentTool.class);
        given(agentToolRegistry.findTool("TIMETABLE")).willReturn(Optional.of(mockTool));
        given(agentToolRegistry.execute(eq("TIMETABLE"), eq(member), anyMap()))
                .willReturn(AgentTool.ToolResult.of("📅 [오늘 첫 수업] 10:00 컴퓨터구조 (공7-301)", null, Map.of("firstClass", "컴퓨터구조")));
        given(mockTool.formatNotification(any(), any())).willReturn("📅 [오늘 첫 수업] 10:00 컴퓨터구조 (공7-301)");

        // when
        agentReminderService.testDispatchCustom(
                member,
                "당일 강의 & 시간표 브리핑",
                "TIMETABLE",
                "{}",
                "🔔 당일 강의 브리핑",
                "",
                "/timetable"
        );

        // then
        verify(fcmService, times(1)).sendDailyBriefNotification(
                eq(1L),
                eq("🔔 당일 강의 브리핑"),
                eq("📅 [오늘 첫 수업] 10:00 컴퓨터구조 (공7-301)"),
                eq(FcmMessageType.AGENT_CUSTOM_REMINDER),
                eq("/timetable")
        );
    }

    @Test
    @DisplayName("스마트 조건: 첫 수업 시작 60분 전(BEFORE_FIRST_CLASS) 매칭 검증")
    void isReminderDue_beforeFirstClass() {
        // given
        Member member = createTestMember(1L);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.of(9, 0); // 09:00

        List<TimeTableService.DailyLectureDto> lectures = List.of(
                new TimeTableService.DailyLectureDto("운영체제", "공7-301", LocalTime.of(10, 0), LocalTime.of(12, 0), "교수A"),
                new TimeTableService.DailyLectureDto("알고리즘", "공7-302", LocalTime.of(13, 0), LocalTime.of(15, 0), "교수B")
        );
        given(timeTableService.getMemberDailyLectures(eq(1L), eq(today))).willReturn(lectures);

        AgentReminder reminder = AgentReminder.builder()
                .member(member)
                .title("첫 수업 안내")
                .targetTool("TIMETABLE")
                .toolParamsJson("{\"triggers\":[{\"type\":\"BEFORE_FIRST_CLASS\",\"minutes\":60}]}")
                .enabled(true)
                .build();

        // when & then
        assertThat(agentReminderService.isReminderDue(reminder, today, now)).isTrue();
        assertThat(agentReminderService.isReminderDue(reminder, today, LocalTime.of(9, 30))).isFalse();
    }

    @Test
    @DisplayName("스마트 조건: 각 수업 시작 10분 전(BEFORE_CLASS) 매칭 검증")
    void isReminderDue_beforeClass() {
        // given
        Member member = createTestMember(1L);
        LocalDate today = LocalDate.now();

        List<TimeTableService.DailyLectureDto> lectures = List.of(
                new TimeTableService.DailyLectureDto("운영체제", "공7-301", LocalTime.of(10, 0), LocalTime.of(12, 0), "교수A"),
                new TimeTableService.DailyLectureDto("알고리즘", "공7-302", LocalTime.of(13, 0), LocalTime.of(15, 0), "교수B")
        );
        given(timeTableService.getMemberDailyLectures(eq(1L), eq(today))).willReturn(lectures);

        AgentReminder reminder = AgentReminder.builder()
                .member(member)
                .title("수업 시작 전 알림")
                .targetTool("TIMETABLE")
                .toolParamsJson("{\"triggers\":[{\"type\":\"BEFORE_CLASS\",\"minutes\":10}]}")
                .enabled(true)
                .build();

        // when & then
        // 10:00 10분 전 = 09:50
        assertThat(agentReminderService.isReminderDue(reminder, today, LocalTime.of(9, 50))).isTrue();
        // 13:00 10분 전 = 12:50
        assertThat(agentReminderService.isReminderDue(reminder, today, LocalTime.of(12, 50))).isTrue();
        // 일치하지 않는 시각
        assertThat(agentReminderService.isReminderDue(reminder, today, LocalTime.of(10, 0))).isFalse();
    }

    @Test
    @DisplayName("스마트 조건: 마지막 수업 종료 10분 후(AFTER_LAST_CLASS) 매칭 검증")
    void isReminderDue_afterLastClass() {
        // given
        Member member = createTestMember(1L);
        LocalDate today = LocalDate.now();

        List<TimeTableService.DailyLectureDto> lectures = List.of(
                new TimeTableService.DailyLectureDto("운영체제", "공7-301", LocalTime.of(10, 0), LocalTime.of(12, 0), "교수A"),
                new TimeTableService.DailyLectureDto("알고리즘", "공7-302", LocalTime.of(13, 0), LocalTime.of(15, 0), "교수B")
        );
        given(timeTableService.getMemberDailyLectures(eq(1L), eq(today))).willReturn(lectures);

        AgentReminder reminder = AgentReminder.builder()
                .member(member)
                .title("하교 버스 알림")
                .targetTool("BUS")
                .toolParamsJson("{\"triggers\":[{\"type\":\"AFTER_LAST_CLASS\",\"offsetMinutes\":10}]}")
                .enabled(true)
                .build();

        // when & then
        // 마지막 수업(15:00) 10분 후 = 15:10
        assertThat(agentReminderService.isReminderDue(reminder, today, LocalTime.of(15, 10))).isTrue();
        assertThat(agentReminderService.isReminderDue(reminder, today, LocalTime.of(15, 0))).isFalse();
    }

    @Test
    @DisplayName("스마트 조건: 공강일(NO_CLASS_DAY) 브리핑 매칭 검증")
    void isReminderDue_noClassDay() {
        // given
        Member member = createTestMember(1L);
        LocalDate today = LocalDate.now();

        given(timeTableService.getMemberDailyLectures(eq(1L), eq(today))).willReturn(Collections.emptyList());

        AgentReminder reminder = AgentReminder.builder()
                .member(member)
                .title("공강일 여유 브리핑")
                .targetTool("SCHEDULE")
                .toolParamsJson("{\"triggers\":[{\"type\":\"NO_CLASS_DAY\",\"time\":\"10:00\"}]}")
                .enabled(true)
                .build();

        // when & then
        assertThat(agentReminderService.isReminderDue(reminder, today, LocalTime.of(10, 0))).isTrue();
        assertThat(agentReminderService.isReminderDue(reminder, today, LocalTime.of(11, 0))).isFalse();
    }
}
