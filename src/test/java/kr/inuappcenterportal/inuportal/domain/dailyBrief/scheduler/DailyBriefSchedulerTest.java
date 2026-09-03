package kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseMeetingService;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.model.DailyBriefSetting;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.repository.DailyBriefSettingRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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
    private CourseMeetingService courseMeetingService;
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

    @Test
    void sendDailyClassBriefing_mergesContinuousMeetings() {
        Member member = Member.builder().studentId("202600001").roles(List.of("ROLE_USER")).build();
        ReflectionTestUtils.setField(member, "id", 1L);
        DailyBriefSetting setting = DailyBriefSetting.createDefault(member);

        Semester semester = org.mockito.Mockito.mock(Semester.class);
        when(semester.getId()).thenReturn(10L);

        TimeTable timeTable = org.mockito.Mockito.mock(TimeTable.class);
        when(timeTable.getId()).thenReturn(100L);

        Course course = org.mockito.Mockito.mock(Course.class);
        when(course.getTitle()).thenReturn("모바일소프트웨어");

        CourseOffering offering = org.mockito.Mockito.mock(CourseOffering.class);
        when(offering.getId()).thenReturn(1000L);
        when(offering.getCourse()).thenReturn(course);

        TimeTableItem item = org.mockito.Mockito.mock(TimeTableItem.class);
        when(item.getCourseOffering()).thenReturn(offering);

        CourseMeeting m1 = org.mockito.Mockito.mock(CourseMeeting.class);
        when(m1.getCourseOffering()).thenReturn(offering);
        CourseMeeting m2 = org.mockito.Mockito.mock(CourseMeeting.class);
        when(m2.getCourseOffering()).thenReturn(offering);

        when(semesterRepository.findFirstByStatusOrderByStartDateDesc(SemesterStatus.OPEN)).thenReturn(Optional.of(semester));
        when(dailyBriefSettingRepository.findAllTimetableDailyBriefByTime(anyString())).thenReturn(List.of(setting));
        when(timeTableRepository.findByMemberIdAndSemesterIdAndIsPrimaryTrue(1L, 10L)).thenReturn(Optional.of(timeTable));
        when(timeTableItemRepository.findAllByTimeTableId(100L)).thenReturn(List.of(item));
        when(courseMeetingRepository.findAllByCourseOfferingIdIn(List.of(1000L))).thenReturn(List.of(m1, m2));

        DayOfWeek today = switch (LocalDate.now().getDayOfWeek()) {
            case MONDAY -> DayOfWeek.MONDAY;
            case TUESDAY -> DayOfWeek.TUESDAY;
            case WEDNESDAY -> DayOfWeek.WEDNESDAY;
            case THURSDAY -> DayOfWeek.THURSDAY;
            case FRIDAY -> DayOfWeek.FRIDAY;
            case SATURDAY -> DayOfWeek.SATURDAY;
            case SUNDAY -> DayOfWeek.SUNDAY;
        };

        CourseMeetingResponseDto mergedDto = new CourseMeetingResponseDto(1L, "정보기술대학 201호", "1,2교시", "01,02", today, LocalTime.of(10, 0), LocalTime.of(11, 50));
        when(courseMeetingService.mergeContinuousMeetings(any())).thenReturn(List.of(mergedDto));

        dailyBriefScheduler.sendDailyClassBriefing();

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(courseMeetingService).mergeContinuousMeetings(any());
        verify(fcmService).sendDailyBriefNotification(
                eq(1L),
                titleCaptor.capture(),
                bodyCaptor.capture(),
                eq(FcmMessageType.DAILY_BRIEF_TIMETABLE),
                eq("/timetable")
        );

        org.assertj.core.api.Assertions.assertThat(titleCaptor.getValue()).isEqualTo("[Daily Brief] 오늘 예정된 강의가 1개 있어요 📚");
        org.assertj.core.api.Assertions.assertThat(bodyCaptor.getValue()).contains("모바일소프트웨어 (10:00~11:50, 정보기술대학 201호)");
    }
}
