package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableDetailItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.TimeTableItemType;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeTableGapAgentTool implements AgentTool {

    private final TimeTableService timeTableService;

    @Override
    public String getName() {
        return "TIMETABLE_GAP";
    }

    @Override
    public String getDescription() {
        return "시간표 공강 시간, 여유 시간, 수업 사이 쉬는 시간, 점심 시간 확보 여부, 우주공강 분석 (params: {\"day\": 요일(1=월~7=일)}). 요일 미지정 시 오늘 분석";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("공강 및 여유 시간을 분석하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        try {
            List<TimeTableResponseDto> timeTables = timeTableService.getTimeTables(member.getId());
            TimeTableResponseDto primary = timeTables.stream()
                    .filter(t -> Boolean.TRUE.equals(t.isPrimary()))
                    .findFirst()
                    .orElse(timeTables.isEmpty() ? null : timeTables.get(0));

            if (primary == null) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("hasTimetable", false);
                UiComponentDto component = UiComponentDto.of("TIMETABLE_GAP", data, "시간표 등록하기", "/timetable");
                return new ToolResult("등록된 시간표가 없습니다. 시간표를 먼저 등록해주세요.", component, data);
            }

            TimeTableDetailResponseDto detail = timeTableService.getTimeTableDetail(member.getId(), primary.id());

            LocalDate today = LocalDate.now();
            int dayNum = today.getDayOfWeek().getValue();
            if (params != null && params.containsKey("day") && params.get("day") != null) {
                try {
                    int pDay = Integer.parseInt(String.valueOf(params.get("day")));
                    if (pDay >= 1 && pDay <= 7) {
                        dayNum = pDay;
                    }
                } catch (Exception ignored) {}
            }

            DayOfWeek targetDay = switch (dayNum) {
                case 1 -> DayOfWeek.MONDAY;
                case 2 -> DayOfWeek.TUESDAY;
                case 3 -> DayOfWeek.WEDNESDAY;
                case 4 -> DayOfWeek.THURSDAY;
                case 5 -> DayOfWeek.FRIDAY;
                case 6 -> DayOfWeek.SATURDAY;
                default -> DayOfWeek.SUNDAY;
            };

            String dayNameKr = switch (targetDay) {
                case MONDAY -> "월";
                case TUESDAY -> "화";
                case WEDNESDAY -> "수";
                case THURSDAY -> "목";
                case FRIDAY -> "금";
                case SATURDAY -> "토";
                case SUNDAY -> "일";
            };

            List<LectureSlot> slots = new ArrayList<>();
            if (detail != null && detail.items() != null) {
                for (TimeTableDetailItemResponseDto item : detail.items()) {
                    String name = "";
                    List<TimeTableMeetingResponseDto> meetings = Collections.emptyList();

                    if (item.type() == TimeTableItemType.COURSE && item.course() != null) {
                        name = item.course().title();
                        meetings = item.course().meetings();
                    } else if (item.type() == TimeTableItemType.CUSTOM && item.customSchedule() != null) {
                        name = item.customSchedule().title();
                        meetings = item.customSchedule().meetings();
                    }

                    if (meetings != null) {
                        for (TimeTableMeetingResponseDto m : meetings) {
                            if (m.day() == targetDay && m.startTime() != null && m.endTime() != null) {
                                slots.add(new LectureSlot(name, m.location() != null ? m.location() : "", m.startTime(), m.endTime()));
                            }
                        }
                    }
                }
            }

            // 시작 시간 순 정렬
            slots.sort(Comparator.comparing(LectureSlot::startTime));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("hasTimetable", true);
            data.put("dayName", dayNameKr);
            data.put("isToday", dayNum == today.getDayOfWeek().getValue());
            data.put("lectureCount", slots.size());

            // 1. 전일 공강인 경우
            if (slots.isEmpty()) {
                data.put("isDayOff", true);
                data.put("totalGapMinutes", 0);
                data.put("gaps", Collections.emptyList());
                data.put("statusText", "전일 공강 🎉");

                UiComponentDto component = UiComponentDto.of("TIMETABLE_GAP", data, "내 시간표 전체보기", "/timetable");
                String summary = String.format("🎉 %s요일은 등록된 강의가 없는 '전일 공강(Day Off)'입니다! 푹 쉬시거나 자유로운 하루를 보내세요.", dayNameKr);
                return new ToolResult(summary, component, data);
            }

            data.put("isDayOff", false);

            // 2. 수업 간 공강(간격) 계산
            List<Map<String, Object>> gapList = new ArrayList<>();
            int totalGapMinutes = 0;
            int totalClassMinutes = 0;
            boolean hasBigGap = false;
            boolean hasLunchGap = false;

            for (int i = 0; i < slots.size(); i++) {
                LectureSlot curr = slots.get(i);
                int duration = curr.durationMinutes();
                totalClassMinutes += duration;

                if (i < slots.size() - 1) {
                    LectureSlot next = slots.get(i + 1);
                    int gapMin = (next.startTime().getHour() * 60 + next.startTime().getMinute())
                            - (curr.endTime().getHour() * 60 + curr.endTime().getMinute());

                    if (gapMin > 15) { // 15분 초과 간격만 유의미한 공강으로 인정
                        totalGapMinutes += gapMin;
                        String gapType = gapMin >= 120 ? "우주공강 🚀" : (gapMin >= 60 ? "식사/여유 공강 🍱" : "단기 휴식 ☕");
                        if (gapMin >= 120) hasBigGap = true;

                        // 점심시간(11:30 ~ 14:00) 겹침 여부
                        int gapStart = curr.endTime().getHour() * 60 + curr.endTime().getMinute();
                        int gapEnd = next.startTime().getHour() * 60 + next.startTime().getMinute();
                        if (gapStart <= 13 * 60 && gapEnd >= 12 * 60) {
                            hasLunchGap = true;
                        }

                        Map<String, Object> gapItem = new LinkedHashMap<>();
                        gapItem.put("beforeLecture", curr.name());
                        gapItem.put("afterLecture", next.name());
                        gapItem.put("startTime", curr.endTime().toString());
                        gapItem.put("endTime", next.startTime().toString());
                        gapItem.put("durationMinutes", gapMin);
                        gapItem.put("durationText", formatMinutes(gapMin));
                        gapItem.put("gapType", gapType);
                        gapList.add(gapItem);
                    }
                }
            }

            data.put("firstClassTime", slots.get(0).startTime().toString());
            data.put("lastClassTime", slots.get(slots.size() - 1).endTime().toString());
            data.put("totalClassMinutes", totalClassMinutes);
            data.put("totalClassText", formatMinutes(totalClassMinutes));
            data.put("totalGapMinutes", totalGapMinutes);
            data.put("totalGapText", formatMinutes(totalGapMinutes));
            data.put("gaps", gapList);
            data.put("hasBigGap", hasBigGap);
            data.put("hasLunchGap", hasLunchGap);

            String statusText = gapList.isEmpty()
                    ? "공강 없음 (연달아 수업)"
                    : (hasBigGap ? "우주공강 있음 (" + formatMinutes(totalGapMinutes) + ")" : "공강 " + formatMinutes(totalGapMinutes));
            data.put("statusText", statusText);

            UiComponentDto component = UiComponentDto.of("TIMETABLE_GAP", data, "내 시간표 전체보기", "/timetable");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s요일 시간표 분석 결과 (총 %d개 수업, 강의 %s):\n",
                    dayNameKr, slots.size(), formatMinutes(totalClassMinutes)));
            if (gapList.isEmpty()) {
                sb.append("• 수업 사이에 15분 이상의 공강이 없습니다. (연강 일정)\n");
            } else {
                sb.append(String.format("• 총 공강 시간: %s (%d개 구간)\n", formatMinutes(totalGapMinutes), gapList.size()));
                for (Map<String, Object> g : gapList) {
                    sb.append(String.format("  - [%s ~ %s] %s (%s, '%s'와 '%s' 사이)\n",
                            g.get("startTime"), g.get("endTime"), g.get("durationText"), g.get("gapType"),
                            g.get("beforeLecture"), g.get("afterLecture")));
                }
            }
            if (hasLunchGap) {
                sb.append("• 🍱 점심 시간대에 넉넉한 공강이 있어 학식을 드실 수 있습니다.");
            } else if (!gapList.isEmpty()) {
                sb.append("• 💡 점심 시간대 전용 공강이 짧거나 없으니 이동 중 식사에 유의하세요.");
            }

            return new ToolResult(sb.toString().trim(), component, data);
        } catch (Exception e) {
            log.error("공강 분석 도구 오류: {}", e.getMessage(), e);
            return new ToolResult("시간표 공강을 분석하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    private String formatMinutes(int min) {
        if (min < 60) return min + "분";
        int h = min / 60;
        int m = min % 60;
        return m == 0 ? (h + "시간") : (h + "시간 " + m + "분");
    }

    private record LectureSlot(
            String name,
            String room,
            LocalTime startTime,
            LocalTime endTime
    ) {
        int durationMinutes() {
            return (endTime.getHour() * 60 + endTime.getMinute()) - (startTime.getHour() * 60 + startTime.getMinute());
        }
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("공강") || lower.contains("쉬는 시간") || lower.contains("우주공강") || lower.contains("여유 시간");
    }
}
