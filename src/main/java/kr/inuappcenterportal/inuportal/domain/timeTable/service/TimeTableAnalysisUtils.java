package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.CourseTimeTableItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.CustomTimeTableItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableDetailItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableMeetingResponseDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class TimeTableAnalysisUtils {

    public record ScheduleSlot(
            String title,
            DayOfWeek day,
            LocalTime startTime,
            LocalTime endTime,
            Integer credit
    ) {}

    public record DayScheduleSummary(
            DayOfWeek day,
            List<ScheduleSlot> slots,
            boolean isFreeDay,
            boolean has9AmClass,
            boolean hasLunchBreak,
            List<String> longGaps, // 2시간 이상 공강 설명
            int maxConsecutiveHours // 최대 연속 수업 시간(대략적)
    ) {}

    public record TimetableSummary(
            int totalCredits,
            int totalClasses,
            int majorCourseCount,
            int generalCourseCount,
            int onlineCourseCount,
            int otherCourseCount,
            List<String> freeDays,
            List<DayScheduleSummary> daySummaries,
            int countOf9AmClasses,
            int totalLongGapsCount,
            boolean hasFridayFree,
            boolean hasMondayFree,
            String rawScheduleText
    ) {}

    /**
     * 시간표 아이템 목록을 바탕으로 고유 해시(SHA-256) 생성
     */
    public static String calculateHash(List<TimeTableDetailItemResponseDto> items) {
        List<String> tokens = new ArrayList<>();

        for (TimeTableDetailItemResponseDto item : items) {
            if (item.course() != null) {
                CourseTimeTableItemResponseDto c = item.course();
                String base = "C:" + c.courseOfferingId() + ":" + c.title() + ":" + c.credit();
                if (c.meetings() != null) {
                    for (TimeTableMeetingResponseDto m : c.meetings()) {
                        tokens.add(base + ":" + m.day() + ":" + m.startTime() + ":" + m.endTime());
                    }
                } else {
                    tokens.add(base);
                }
            } else if (item.customSchedule() != null) {
                CustomTimeTableItemResponseDto cs = item.customSchedule();
                String base = "U:" + cs.customScheduleId() + ":" + cs.title();
                if (cs.meetings() != null) {
                    for (TimeTableMeetingResponseDto m : cs.meetings()) {
                        tokens.add(base + ":" + m.day() + ":" + m.startTime() + ":" + m.endTime());
                    }
                } else {
                    tokens.add(base);
                }
            }
        }

        Collections.sort(tokens);
        String combined = String.join("|", tokens);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(combined.hashCode());
        }
    }

    /**
     * 시간표의 구조적 특징 및 메트릭 분석
     */
    public static TimetableSummary analyzeTimetable(
            List<TimeTableDetailItemResponseDto> items,
            Map<Long, kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering> offeringMap
    ) {
        List<ScheduleSlot> allSlots = new ArrayList<>();
        int totalCredits = 0;
        int totalClasses = 0;
        int majorCourseCount = 0;
        int generalCourseCount = 0;
        int onlineCourseCount = 0;
        int otherCourseCount = 0;

        for (TimeTableDetailItemResponseDto item : items) {
            if (item.course() != null) {
                CourseTimeTableItemResponseDto c = item.course();
                totalCredits += (c.credit() != null ? c.credit() : 0);
                totalClasses++;

                // 전공/교양/이러닝 판별
                kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering offering =
                        offeringMap != null && c.courseOfferingId() != null ? offeringMap.get(c.courseOfferingId()) : null;

                if (offering != null) {
                    // 이러닝/온라인 여부 체크
                    boolean isOnline = false;
                    if (offering.getSsupTypeName() != null) {
                        switch (offering.getSsupTypeName()) {
                            case E_LEARNING, E_LEARNING_HUSS, K_MOOC, OCU, BLENDED_ONLINE_COURSE, BLENDED_ONLINE_COURSE_HUSS -> isOnline = true;
                            default -> {}
                        }
                    }
                    if (!isOnline && offering.getSsupTypeNameRaw() != null) {
                        String raw = offering.getSsupTypeNameRaw().toLowerCase();
                        if (raw.contains("e-learning") || raw.contains("온라인") || raw.contains("사이버") || raw.contains("k-mooc") || raw.contains("ocu")) {
                            isOnline = true;
                        }
                    }
                    if (isOnline) {
                        onlineCourseCount++;
                    }

                    // 전공/교양 구분
                    boolean isMajor = false;
                    boolean isGeneral = false;

                    if (offering.getIsuName() != null) {
                        switch (offering.getIsuName()) {
                            case MAJOR_FOUNDATION, MAJOR_CORE, MAJOR_ADVANCED -> isMajor = true;
                            case BASIC_LIBERAL_ARTS, CORE_LIBERAL_ARTS, ADVANCED_LIBERAL_ARTS, GENERAL_ELECTIVE -> isGeneral = true;
                            default -> {}
                        }
                    }
                    if (!isMajor && !isGeneral && offering.getIsuNameRaw() != null) {
                        if (offering.getIsuNameRaw().contains("전공")) isMajor = true;
                        else if (offering.getIsuNameRaw().contains("교양")) isGeneral = true;
                    }

                    if (isMajor) majorCourseCount++;
                    else if (isGeneral) generalCourseCount++;
                    else otherCourseCount++;
                } else {
                    otherCourseCount++;
                }

                if (c.meetings() != null) {
                    for (TimeTableMeetingResponseDto m : c.meetings()) {
                        if (m.day() != null && m.startTime() != null && m.endTime() != null) {
                            allSlots.add(new ScheduleSlot(c.title(), m.day(), m.startTime(), m.endTime(), c.credit()));
                        }
                    }
                }
            } else if (item.customSchedule() != null) {
                CustomTimeTableItemResponseDto cs = item.customSchedule();
                totalClasses++;
                otherCourseCount++;
                if (cs.meetings() != null) {
                    for (TimeTableMeetingResponseDto m : cs.meetings()) {
                        if (m.day() != null && m.startTime() != null && m.endTime() != null) {
                            allSlots.add(new ScheduleSlot(cs.title(), m.day(), m.startTime(), m.endTime(), 0));
                        }
                    }
                }
            }
        }

        DayOfWeek[] weekdays = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
        List<DayScheduleSummary> daySummaries = new ArrayList<>();
        List<String> freeDays = new ArrayList<>();
        int countOf9Am = 0;
        int totalLongGaps = 0;

        StringBuilder rawText = new StringBuilder();

        for (DayOfWeek day : weekdays) {
            List<ScheduleSlot> daySlots = allSlots.stream()
                    .filter(s -> s.day() == day)
                    .sorted(Comparator.comparing(ScheduleSlot::startTime))
                    .toList();

            String dayKorean = toKoreanDay(day);

            if (daySlots.isEmpty()) {
                freeDays.add(dayKorean);
                daySummaries.add(new DayScheduleSummary(day, daySlots, true, false, true, List.of(), 0));
                rawText.append(String.format("- %s: 공강 (수업 없음)\n", dayKorean));
                continue;
            }

            rawText.append(String.format("- %s: ", dayKorean));
            String slotDescriptions = daySlots.stream()
                    .map(s -> String.format("%s(%s~%s)", s.title(), s.startTime(), s.endTime()))
                    .collect(Collectors.joining(", "));
            rawText.append(slotDescriptions).append("\n");

            boolean has9Am = daySlots.stream().anyMatch(s -> s.startTime().isBefore(LocalTime.of(9, 30)));
            if (has9Am) countOf9Am++;

            // 점심시간 체크 (12:00 ~ 13:00 사이에 수업이 없는지)
            boolean hasLunchBreak = daySlots.stream().noneMatch(s ->
                    !s.endTime().isBefore(LocalTime.of(12, 30)) && !s.startTime().isAfter(LocalTime.of(12, 30))
            );

            // 공강 시간(Gap) 분석
            List<String> longGaps = new ArrayList<>();
            for (int i = 0; i < daySlots.size() - 1; i++) {
                ScheduleSlot current = daySlots.get(i);
                ScheduleSlot next = daySlots.get(i + 1);
                long minutes = Duration.between(current.endTime(), next.startTime()).toMinutes();
                if (minutes >= 120) {
                    long hours = minutes / 60;
                    long remainMin = minutes % 60;
                    String gapStr = remainMin > 0 ? String.format("%d시간 %d분", hours, remainMin) : String.format("%d시간", hours);
                    longGaps.add(String.format("%s ~ %s 사이 %s 공강", current.title(), next.title(), gapStr));
                    totalLongGaps++;
                }
            }

            daySummaries.add(new DayScheduleSummary(day, daySlots, false, has9Am, hasLunchBreak, longGaps, 0));
        }

        boolean hasFridayFree = freeDays.contains("금요일");
        boolean hasMondayFree = freeDays.contains("월요일");

        return new TimetableSummary(
                totalCredits,
                totalClasses,
                majorCourseCount,
                generalCourseCount,
                onlineCourseCount,
                otherCourseCount,
                freeDays,
                daySummaries,
                countOf9Am,
                totalLongGaps,
                hasFridayFree,
                hasMondayFree,
                rawText.toString()
        );
    }

    public static String toKoreanDay(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }
}
