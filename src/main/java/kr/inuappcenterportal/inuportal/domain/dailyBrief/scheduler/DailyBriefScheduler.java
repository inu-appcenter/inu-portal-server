package kr.inuappcenterportal.inuportal.domain.dailyBrief.scheduler;

import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.enums.ScheduleScope;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyBriefScheduler {

    private final DailyBriefSettingRepository dailyBriefSettingRepository;
    private final FcmService fcmService;
    private final TimeTableRepository timeTableRepository;
    private final TimeTableItemRepository timeTableItemRepository;
    private final CourseMeetingRepository courseMeetingRepository;
    private final CustomScheduleMeetingRepository customScheduleMeetingRepository;
    private final SemesterRepository semesterRepository;
    private final ScheduleRepository scheduleRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 1. 수업 시작 전 알림 (5분마다 실행)
     */
    @Scheduled(cron = "0 */5 * * * MON-FRI", zone = "Asia/Seoul")
    @Transactional(readOnly = true)
    public void sendPreClassAlerts() {
        Optional<Semester> openSemesterOpt = semesterRepository.findFirstByStatusOrderByStartDateDesc(SemesterStatus.OPEN);
        if (openSemesterOpt.isEmpty()) {
            return;
        }

        Semester currentSemester = openSemesterOpt.get();
        DayOfWeek today = toDomainDayOfWeek(LocalDate.now().getDayOfWeek());
        if (today == null) {
            return;
        }

        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<DailyBriefSetting> settings = dailyBriefSettingRepository.findAllTimetablePreAlertEnabled();

        for (DailyBriefSetting setting : settings) {
            try {
                Member member = setting.getMember();
                int alertMinutes = setting.getTimetablePreAlertMinutes();

                Optional<TimeTable> primaryTableOpt = timeTableRepository.findByMemberIdAndSemesterIdAndIsPrimaryTrue(
                        member.getId(), currentSemester.getId()
                );
                if (primaryTableOpt.isEmpty()) {
                    continue;
                }

                TimeTable timeTable = primaryTableOpt.get();
                List<TimeTableItem> items = timeTableItemRepository.findAllByTimeTableId(timeTable.getId());

                List<ClassScheduleEntry> todayEntries = extractTodayClassEntries(items, today);

                for (ClassScheduleEntry entry : todayEntries) {
                    long diffMinutes = ChronoUnit.MINUTES.between(now, entry.startTime());
                    // 오차 범위 2분 이내 매칭 (5분 단위 스케줄러 고려)
                    if (Math.abs(diffMinutes - alertMinutes) <= 2) {
                        String alertTimeText = (alertMinutes >= 60 && alertMinutes % 60 == 0)
                                ? (alertMinutes / 60) + "시간"
                                : (alertMinutes >= 60)
                                    ? (alertMinutes / 60) + "시간 " + (alertMinutes % 60) + "분"
                                    : alertMinutes + "분";
                        String title = String.format("%s 후 수업이 시작돼요.", alertTimeText);
                        String locationInfo = (entry.location() != null && !entry.location().isBlank())
                                ? ", " + entry.location()
                                : "";
                        String body = String.format("%s (%s~%s%s)",
                                entry.title(),
                                entry.startTime().format(TIME_FORMATTER),
                                entry.endTime().format(TIME_FORMATTER),
                                locationInfo);

                        fcmService.sendDailyBriefNotification(
                                member.getId(),
                                title,
                                body,
                                FcmMessageType.DAILY_BRIEF_TIMETABLE,
                                "/timetable"
                        );
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send pre-class alert for memberId={}: {}", setting.getMember().getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 2. 당일 강의 목록 묶음 브리핑 (10분마다 실행)
     */
    @Scheduled(cron = "0 0/10 7-22 * * MON-FRI", zone = "Asia/Seoul")
    @Transactional(readOnly = true)
    public void sendDailyClassBriefing() {
        Optional<Semester> openSemesterOpt = semesterRepository.findFirstByStatusOrderByStartDateDesc(SemesterStatus.OPEN);
        if (openSemesterOpt.isEmpty()) {
            return;
        }

        Semester currentSemester = openSemesterOpt.get();
        DayOfWeek today = toDomainDayOfWeek(LocalDate.now().getDayOfWeek());
        if (today == null) {
            return;
        }

        String currentTimeStr = LocalTime.now().format(TIME_FORMATTER);
        List<DailyBriefSetting> settings = dailyBriefSettingRepository.findAllTimetableDailyBriefByTime(currentTimeStr);

        for (DailyBriefSetting setting : settings) {
            try {
                Member member = setting.getMember();
                Optional<TimeTable> primaryTableOpt = timeTableRepository.findByMemberIdAndSemesterIdAndIsPrimaryTrue(
                        member.getId(), currentSemester.getId()
                );
                if (primaryTableOpt.isEmpty()) {
                    continue;
                }

                TimeTable timeTable = primaryTableOpt.get();
                List<TimeTableItem> items = timeTableItemRepository.findAllByTimeTableId(timeTable.getId());
                List<ClassScheduleEntry> todayEntries = extractTodayClassEntries(items, today);

                if (todayEntries.isEmpty()) {
                    continue;
                }

                // 시작 시간 순 정렬
                todayEntries.sort(Comparator.comparing(ClassScheduleEntry::startTime));

                String title = String.format("[Daily Brief] 오늘 예정된 강의가 %d개 있어요 📚", todayEntries.size());
                StringBuilder bodyBuilder = new StringBuilder();
                for (int i = 0; i < todayEntries.size(); i++) {
                    ClassScheduleEntry entry = todayEntries.get(i);
                    String locationInfo = (entry.location() != null && !entry.location().isBlank())
                            ? ", " + entry.location()
                            : "";
                    bodyBuilder.append(String.format("%d. %s (%s~%s%s)",
                            i + 1,
                            entry.title(),
                            entry.startTime().format(TIME_FORMATTER),
                            entry.endTime().format(TIME_FORMATTER),
                            locationInfo
                    ));
                    if (i < todayEntries.size() - 1) {
                        bodyBuilder.append("\n");
                    }
                }

                fcmService.sendDailyBriefNotification(
                        member.getId(),
                        title,
                        bodyBuilder.toString(),
                        FcmMessageType.DAILY_BRIEF_TIMETABLE,
                        "/timetable"
                );
            } catch (Exception e) {
                log.error("Failed to send daily class brief for memberId={}: {}", setting.getMember().getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 3. 당일 학사/학과 일정 브리핑 (10분마다 실행)
     */
    @Scheduled(cron = "0 0/10 7-22 * * *", zone = "Asia/Seoul")
    @Transactional(readOnly = true)
    public void sendDailyScheduleBriefing() {
        LocalDate today = LocalDate.now();
        List<Schedule> todayAllSchedules = scheduleRepository.findAllByDateIncluded(today);
        if (todayAllSchedules.isEmpty()) {
            return;
        }

        String currentTimeStr = LocalTime.now().format(TIME_FORMATTER);
        List<DailyBriefSetting> settings = dailyBriefSettingRepository.findAllScheduleDailyBriefByTime(currentTimeStr);

        for (DailyBriefSetting setting : settings) {
            try {
                Member member = setting.getMember();
                ScheduleScope scope = setting.getScheduleScope();

                List<Schedule> targetSchedules = todayAllSchedules.stream().filter(s -> {
                    boolean isSchool = (s.getDepartment() == null);
                    boolean isMyDept = (s.getDepartment() != null && member.getDepartment() != null
                            && s.getDepartment().equals(member.getDepartment()));

                    if (scope == ScheduleScope.SCHOOL_ONLY) {
                        return isSchool;
                    } else if (scope == ScheduleScope.DEPT_ONLY) {
                        return isMyDept;
                    } else {
                        // ALL
                        return isSchool || isMyDept;
                    }
                }).toList();

                if (targetSchedules.isEmpty()) {
                    continue;
                }

                String title = "[Daily Brief] 오늘의 학사일정을 확인하세요 🗓️";
                StringBuilder bodyBuilder = new StringBuilder();
                for (int i = 0; i < targetSchedules.size(); i++) {
                    Schedule s = targetSchedules.get(i);
                    String categoryLabel = (s.getDepartment() == null) ? "학교" : s.getDepartment().getDepartmentName();
                    bodyBuilder.append(String.format("• [%s] %s", categoryLabel, s.getContent()));
                    if (i < targetSchedules.size() - 1) {
                        bodyBuilder.append("\n");
                    }
                }

                fcmService.sendDailyBriefNotification(
                        member.getId(),
                        title,
                        bodyBuilder.toString(),
                        FcmMessageType.DAILY_BRIEF_SCHEDULE,
                        "/home/calendar"
                );
            } catch (Exception e) {
                log.error("Failed to send daily schedule brief for memberId={}: {}", setting.getMember().getId(), e.getMessage(), e);
            }
        }
    }

    private List<ClassScheduleEntry> extractTodayClassEntries(List<TimeTableItem> items, DayOfWeek today) {
        List<ClassScheduleEntry> entries = new ArrayList<>();

        List<Long> courseOfferingIds = items.stream()
                .filter(i -> i.getCourseOffering() != null)
                .map(i -> i.getCourseOffering().getId())
                .toList();

        if (!courseOfferingIds.isEmpty()) {
            List<CourseMeeting> courseMeetings = courseMeetingRepository.findAllByCourseOfferingIdIn(courseOfferingIds);
            for (TimeTableItem item : items) {
                if (item.getCourseOffering() == null) continue;
                String title = item.getCourseOffering().getCourse().getTitle();

                for (CourseMeeting m : courseMeetings) {
                    if (m.getCourseOffering().getId().equals(item.getCourseOffering().getId()) && m.getDay() == today) {
                        entries.add(new ClassScheduleEntry(title, m.getLocation(), m.getStartTime(), m.getEndTime()));
                    }
                }
            }
        }

        List<Long> customScheduleIds = items.stream()
                .filter(i -> i.getCustomSchedule() != null)
                .map(i -> i.getCustomSchedule().getId())
                .toList();

        if (!customScheduleIds.isEmpty()) {
            List<CustomScheduleMeeting> customMeetings = customScheduleMeetingRepository.findAllByCustomScheduleIdIn(customScheduleIds);
            for (TimeTableItem item : items) {
                if (item.getCustomSchedule() == null) continue;
                String title = item.getCustomSchedule().getTitle();

                for (CustomScheduleMeeting m : customMeetings) {
                    if (m.getCustomSchedule().getId().equals(item.getCustomSchedule().getId()) && m.getDay() == today) {
                        entries.add(new ClassScheduleEntry(title, m.getLocation(), m.getStartTime(), m.getEndTime()));
                    }
                }
            }
        }

        return entries;
    }

    private DayOfWeek toDomainDayOfWeek(java.time.DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DayOfWeek.MONDAY;
            case TUESDAY -> DayOfWeek.TUESDAY;
            case WEDNESDAY -> DayOfWeek.WEDNESDAY;
            case THURSDAY -> DayOfWeek.THURSDAY;
            case FRIDAY -> DayOfWeek.FRIDAY;
            case SATURDAY -> DayOfWeek.SATURDAY;
            case SUNDAY -> DayOfWeek.SUNDAY;
        };
    }

    private record ClassScheduleEntry(
            String title,
            String location,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
