package kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler;

import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.model.DailyBriefSetting;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.repository.DailyBriefSettingRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyBriefSchedulerTest {

    @Mock
    private DailyBriefSettingRepository dailyBriefSettingRepository;
    @Mock
    private FcmService fcmService;
    @Mock
    private TimeTableRepository timeTableRepository;
    @Mock
    private TimeTableItemRepository timeTableItemRepository;
    @Mock
    private CourseMeetingRepository courseMeetingRepository;
    @Mock
    private CustomScheduleMeetingRepository customScheduleMeetingRepository;
    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private DailyBriefScheduler dailyBriefScheduler;

    @Test
    void dailyScheduleBriefing_targetsOnlySchedulesStartingToday() {
        when(scheduleRepository.findAllStartingOn(any(LocalDate.class))).thenReturn(List.of());

        dailyBriefScheduler.sendDailyScheduleBriefing();

        verify(scheduleRepository).findAllStartingOn(any(LocalDate.class));
        verify(scheduleRepository, never()).findAllByDateIncluded(any(LocalDate.class));
    }

    @Test
    void dailyScheduleBriefing_includesEachSchedulePeriodInNotification() {
        LocalDate today = LocalDate.now();
        Member member = Member.builder().studentId("202600001").roles(List.of("ROLE_USER")).build();
        ReflectionTestUtils.setField(member, "id", 1L);
        DailyBriefSetting setting = DailyBriefSetting.createDefault(member);

        Schedule oneDaySchedule = Schedule.builder()
                .id(1L)
                .startDate(today)
                .endDate(today)
                .content("수강신청")
                .build();
        Schedule multiDaySchedule = Schedule.builder()
                .id(2L)
                .startDate(today)
                .endDate(today.plusDays(2))
                .content("등록금 납부")
                .build();

        when(scheduleRepository.findAllStartingOn(any(LocalDate.class)))
                .thenReturn(List.of(oneDaySchedule, multiDaySchedule));
        when(dailyBriefSettingRepository.findAllScheduleDailyBriefByTime(anyString()))
                .thenReturn(List.of(setting));

        dailyBriefScheduler.sendDailyScheduleBriefing();

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fcmService).sendDailyBriefNotification(
                eq(1L),
                eq("오늘부터 시작되는 학사일정이에요."),
                bodyCaptor.capture(),
                eq(FcmMessageType.DAILY_BRIEF_SCHEDULE),
                eq("/home/calendar")
        );
        org.assertj.core.api.Assertions.assertThat(bodyCaptor.getValue())
                .isEqualTo("• [학교] 수강신청 (%s)\n• [학교] 등록금 납부 (%s)".formatted(
                        "%d월 %d일".formatted(today.getMonthValue(), today.getDayOfMonth()),
                        "%d월 %d일 ~ %d월 %d일".formatted(
                                today.getMonthValue(), today.getDayOfMonth(),
                                today.plusDays(2).getMonthValue(), today.plusDays(2).getDayOfMonth()
                        )
                ));
    }
}
