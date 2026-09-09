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
public class TimeTableAgentTool implements AgentTool {

    private final TimeTableService timeTableService;

    @Override
    public String getName() {
        return "TIMETABLE";
    }

    @Override
    public String getDescription() {
        return "내 시간표, 오늘 수업, 강의실, 다음 강의 관련 질문 (params: 없음)";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("시간표를 확인하려면 로그인이 필요합니다. 로그인 후 다시 질문해주세요.", 
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
                UiComponentDto component = UiComponentDto.of("TIMETABLE", data, "시간표 생성하기", "/timetable");
                return new ToolResult("등록된 시간표가 없습니다. 시간표 메뉴에서 새 시간표를 등록해보세요.", component, data);
            }

            TimeTableDetailResponseDto detail = timeTableService.getTimeTableDetail(member.getId(), primary.id());

            LocalDate today = LocalDate.now();
            java.time.DayOfWeek jDay = today.getDayOfWeek();
            DayOfWeek targetDay;
            try {
                targetDay = DayOfWeek.valueOf(jDay.name());
            } catch (Exception e) {
                targetDay = null;
            }

            String dayNameKr = switch (jDay) {
                case MONDAY -> "월";
                case TUESDAY -> "화";
                case WEDNESDAY -> "수";
                case THURSDAY -> "목";
                case FRIDAY -> "금";
                case SATURDAY -> "토";
                case SUNDAY -> "일";
            };
            String todayDateText = String.format("%d월 %d일 (%s) 오늘의 시간표",
                    today.getMonthValue(), today.getDayOfMonth(), dayNameKr);

            List<Map<String, Object>> todayClassList = new ArrayList<>();
            if (detail != null && detail.items() != null && targetDay != null) {
                for (TimeTableDetailItemResponseDto item : detail.items()) {
                    String name = "";
                    String professor = "";
                    List<TimeTableMeetingResponseDto> meetings = Collections.emptyList();

                    if (item.type() == TimeTableItemType.COURSE && item.course() != null) {
                        name = item.course().title();
                        professor = item.course().professor();
                        meetings = item.course().meetings();
                    } else if (item.type() == TimeTableItemType.CUSTOM && item.customSchedule() != null) {
                        name = item.customSchedule().title();
                        meetings = item.customSchedule().meetings();
                    }

                    if (meetings != null) {
                        for (TimeTableMeetingResponseDto m : meetings) {
                            if (m.day() == targetDay) {
                                Map<String, Object> c = new LinkedHashMap<>();
                                c.put("name", name);
                                c.put("room", m.location() != null ? m.location() : "");
                                c.put("startTime", m.startTime() != null ? m.startTime().toString() : "");
                                c.put("endTime", m.endTime() != null ? m.endTime().toString() : "");
                                c.put("professor", professor != null ? professor : "");
                                c.put("memo", item.memo() != null ? item.memo() : "");
                                todayClassList.add(c);
                            }
                        }
                    }
                }
            }

            todayClassList.sort(Comparator.comparing(a -> String.valueOf(a.get("startTime"))));

            LocalTime now = LocalTime.now();
            int nowMinutes = now.getHour() * 60 + now.getMinute();
            String timetableStatusText;
            boolean hasOngoing = false;
            Integer minutesUntilNext = null;

            for (Map<String, Object> c : todayClassList) {
                String startStr = (String) c.get("startTime");
                String endStr = (String) c.get("endTime");
                boolean isCurrent = false;
                if (startStr != null && !startStr.isBlank() && endStr != null && !endStr.isBlank()) {
                    try {
                        LocalTime s = LocalTime.parse(startStr);
                        LocalTime e = LocalTime.parse(endStr);
                        int sMin = s.getHour() * 60 + s.getMinute();
                        int eMin = e.getHour() * 60 + e.getMinute();
                        if (sMin <= nowMinutes && nowMinutes < eMin) {
                            isCurrent = true;
                            hasOngoing = true;
                        } else if (sMin > nowMinutes) {
                            int diff = sMin - nowMinutes;
                            if (minutesUntilNext == null || diff < minutesUntilNext) {
                                minutesUntilNext = diff;
                            }
                        }
                    } catch (Exception ignored) {}
                }
                c.put("isCurrent", isCurrent);
            }

            if (todayClassList.isEmpty()) {
                timetableStatusText = "등록된 수업 없음";
            } else if (hasOngoing) {
                timetableStatusText = "진행 중";
            } else if (minutesUntilNext != null) {
                if (minutesUntilNext < 60) {
                    timetableStatusText = minutesUntilNext + "분 후 시작";
                } else {
                    int h = minutesUntilNext / 60;
                    int m = minutesUntilNext % 60;
                    timetableStatusText = (m == 0) ? (h + "시간 후 시작") : (h + "시간 " + m + "분 후 시작");
                }
            } else {
                timetableStatusText = "오늘 수업 끝";
            }

            StringBuilder summary = new StringBuilder();
            if (todayClassList.isEmpty()) {
                summary.append(String.format("오늘(%s)은 '%s' 시간표에 등록된 수업이 없습니다! (상태: %s)",
                        dayNameKr, primary.timeTableName(), timetableStatusText));
            } else {
                summary.append(String.format("오늘(%s) '%s' 시간표의 강의 일정입니다 (상태: %s):\n",
                        dayNameKr, primary.timeTableName(), timetableStatusText));
                for (Map<String, Object> c : todayClassList) {
                    String currentTag = Boolean.TRUE.equals(c.get("isCurrent")) ? " [현재 진행 중]" : "";
                    summary.append(String.format("• [%s ~ %s] %s (강의실: %s%s)%s\n",
                            c.get("startTime"), c.get("endTime"), c.get("name"),
                            c.get("room"),
                            (c.get("professor") != null && !((String)c.get("professor")).isBlank()) ? ", " + c.get("professor") + " 교수" : "",
                            currentTag));
                }
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("hasTimetable", true);
            data.put("timeTableName", primary.timeTableName());
            data.put("year", primary.year());
            data.put("term", primary.term());
            data.put("todayDateText", todayDateText);
            data.put("statusText", timetableStatusText);
            data.put("todayClasses", todayClassList);

            UiComponentDto component = UiComponentDto.of("TIMETABLE", data, "내 시간표 전체보기", "/timetable");
            return new ToolResult(summary.toString().trim(), component, data);
        } catch (Exception e) {
            log.error("시간표 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("시간표 정보를 조회하는 도중 오류가 발생했습니다.", null, null);
        }
    }
}
