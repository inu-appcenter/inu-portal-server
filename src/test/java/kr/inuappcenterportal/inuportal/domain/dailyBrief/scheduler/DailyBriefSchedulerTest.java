package kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler;

import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.repository.DailyBriefSettingRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
}
