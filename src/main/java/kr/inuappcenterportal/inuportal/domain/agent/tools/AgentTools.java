package kr.inuappcenterportal.inuportal.domain.agent.tools;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusArrivalItemDto;
import kr.inuappcenterportal.inuportal.domain.bus.dto.BusStopAliasDto;
import kr.inuappcenterportal.inuportal.domain.bus.service.BusService;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.directory.dto.CollegeOfficeContactResponse;
import kr.inuappcenterportal.inuportal.domain.directory.service.CollegeOfficeContactService;
import kr.inuappcenterportal.inuportal.domain.directory.service.DirectoryService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.notice.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.schedule.dto.ScheduleResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableDetailItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.TimeTableItemType;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableService;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.req.DailyBriefSettingRequestDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.res.DailyBriefSettingResponseDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.enums.ScheduleScope;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.service.DailyBriefService;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.keyword.dto.res.KeywordResponse;
import kr.inuappcenterportal.inuportal.domain.keyword.service.KeywordService;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.weather.dto.WeatherResponseDto;
import kr.inuappcenterportal.inuportal.domain.weather.service.WeatherService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentTools {

    private final WeatherService weatherService;
    private final CafeteriaService cafeteriaService;
    private final BusService busService;
    private final TimeTableService timeTableService;
    private final ScheduleService scheduleService;
    private final NoticeService noticeService;
    private final CollegeOfficeContactService collegeOfficeContactService;
    private final DirectoryService directoryService;
    private final MemberService memberService;
    private final KeywordService keywordService;
    private final DailyBriefService dailyBriefService;

    private static final List<String> ALL_CAFETERIAS = List.of(
            "학생식당", "제1기숙사식당", "27호관식당", "2호관(교직원)식당", "사범대식당", "2기숙사 식당"
    );

    /**
     * 날씨 도구 실행
     */
    public ToolResult executeWeather() {
        try {
            WeatherResponseDto weather = weatherService.getWeather();
            UiComponentDto component = UiComponentDto.of("WEATHER", weather, "송도 캠퍼스 날씨 홈", "/home");
            String summary = String.format("연수구 송도동은 현재 %s 상태이며 기온은 %s입니다. 미세먼지 등급은 %s입니다.",
                    weather.getSky(), weather.getTemperature(), weather.getPm10Grade());
            return new ToolResult(summary, component, weather);
        } catch (Exception e) {
            log.error("날씨 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("날씨 정보를 가져오는 데 실패했습니다.", null, null);
        }
    }

    /**
     * 학식 도구 실행
     */
    public ToolResult executeCafeteria(Map<String, Object> params) {
        try {
            boolean isAll = true;
            String cafeteria = "학생식당";

            if (params != null && params.containsKey("cafeteria") && params.get("cafeteria") != null) {
                String candidate = String.valueOf(params.get("cafeteria")).trim();
                if (!candidate.isBlank() && !candidate.contains("전체") && !"ALL".equalsIgnoreCase(candidate)) {
                    cafeteria = normalizeCafeteriaName(candidate);
                    isAll = false;
                }
            }

            int dayOfWeek = LocalDate.now().getDayOfWeek().getValue(); // 월=1 ~ 일=7
            if (params != null && params.containsKey("day") && params.get("day") != null) {
                try {
                    int customDay = Integer.parseInt(String.valueOf(params.get("day")));
                    if (customDay >= 1 && customDay <= 7) {
                        dayOfWeek = customDay;
                    }
                } catch (NumberFormatException ignored) {}
            }

            LocalTime now = LocalTime.now();
            int slotIndex;
            String mealName;

            String mealTypeParam = (params != null && params.get("mealType") != null)
                    ? String.valueOf(params.get("mealType")).toUpperCase().trim()
                    : "AUTO";

            if ("BREAKFAST".equals(mealTypeParam) || "조식".equals(mealTypeParam) || "아침".equals(mealTypeParam)) {
                slotIndex = 0;
                mealName = "조식(아침)";
            } else if ("LUNCH".equals(mealTypeParam) || "중식".equals(mealTypeParam) || "점심".equals(mealTypeParam)) {
                slotIndex = 1;
                mealName = "중식(점심)";
            } else if ("DINNER".equals(mealTypeParam) || "석식".equals(mealTypeParam) || "저녁".equals(mealTypeParam)) {
                slotIndex = 2;
                mealName = "석식(저녁)";
            } else {
                if (now.isBefore(LocalTime.of(9, 30))) {
                    slotIndex = 0;
                    mealName = "조식(아침)";
                } else if (now.isBefore(LocalTime.of(14, 0))) {
                    slotIndex = 1;
                    mealName = "중식(점심)";
                } else {
                    slotIndex = 2;
                    mealName = "석식(저녁)";
                }
            }

            if (isAll) {
                List<Map<String, Object>> cafeteriaList = new ArrayList<>();
                StringBuilder summary = new StringBuilder();
                summary.append(String.format("[현재 시간대 기준: %s] 전 캠퍼스 식당 메뉴 현황:\n", mealName));

                for (String name : ALL_CAFETERIAS) {
                    List<String> menus = cafeteriaService.getCafeteria(name, dayOfWeek);
                    String menu = (menus.size() > slotIndex) ? menus.get(slotIndex) : "-";
                    boolean isOperated = !"-".equals(menu) && !menu.isBlank() && !menu.contains("쉬는 날") && !menu.contains("오늘은 쉽니다");

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", name);
                    item.put("menu", menu);
                    item.put("isOperated", isOperated);
                    cafeteriaList.add(item);

                    if (isOperated) {
                        String cleanMenu = menu.replaceAll("\\n+", " | ").trim();
                        summary.append(String.format("• [%s]: %s\n", name, cleanMenu));
                    }
                }

                Map<String, Object> cafeteriaData = new LinkedHashMap<>();
                cafeteriaData.put("isAllCafeterias", true);
                cafeteriaData.put("mealLabel", mealName);
                cafeteriaData.put("dayOfWeek", dayOfWeek);
                cafeteriaData.put("cafeterias", cafeteriaList);

                UiComponentDto component = UiComponentDto.of("CAFETERIA", cafeteriaData, "학식 식단표 전체보기", "/home/menu");
                return new ToolResult(summary.toString().trim(), component, cafeteriaData);
            } else {
                List<String> menuList = cafeteriaService.getCafeteria(cafeteria, dayOfWeek);

                Map<String, Object> cafeteriaData = new LinkedHashMap<>();
                cafeteriaData.put("isAllCafeterias", false);
                cafeteriaData.put("cafeteria", cafeteria);
                cafeteriaData.put("dayOfWeek", dayOfWeek);
                cafeteriaData.put("currentMealLabel", mealName);
                cafeteriaData.put("breakfast", menuList.size() > 0 ? menuList.get(0) : "-");
                cafeteriaData.put("lunch", menuList.size() > 1 ? menuList.get(1) : "-");
                cafeteriaData.put("dinner", menuList.size() > 2 ? menuList.get(2) : "-");

                UiComponentDto component = UiComponentDto.of("CAFETERIA", cafeteriaData, "학식 식단표 전체보기", "/home/menu");

                String mealContent = switch (slotIndex) {
                    case 0 -> String.valueOf(cafeteriaData.get("breakfast"));
                    case 2 -> String.valueOf(cafeteriaData.get("dinner"));
                    default -> String.valueOf(cafeteriaData.get("lunch"));
                };

                String summary = String.format("%s의 오늘 [%s] 메뉴입니다:\n%s",
                        cafeteria, mealName, mealContent);
                return new ToolResult(summary, component, cafeteriaData);
            }
        } catch (Exception e) {
            log.error("학식 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("학식 메뉴를 조회하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    /**
     * 버스 실시간 도착 도구 실행
     */
    public ToolResult executeBus(Map<String, Object> params) {
        try {
            String targetStopName = "정문";
            if (params != null && params.containsKey("stopName") && params.get("stopName") != null) {
                targetStopName = String.valueOf(params.get("stopName")).trim();
            }

            List<BusStopAliasDto> aliases = busService.getStopAliases();
            String matchedBstopId = null;
            String resolvedStopName = targetStopName;

            for (BusStopAliasDto alias : aliases) {
                if (alias.getStopAlias().contains(targetStopName) || targetStopName.contains(alias.getStopAlias()) ||
                    (alias.getBstopName() != null && alias.getBstopName().contains(targetStopName))) {
                    matchedBstopId = alias.getBstopId();
                    resolvedStopName = alias.getStopAlias();
                    break;
                }
            }

            if (matchedBstopId == null && !aliases.isEmpty()) {
                matchedBstopId = aliases.get(0).getBstopId();
                resolvedStopName = aliases.get(0).getStopAlias();
            }

            List<BusArrivalItemDto> arrivals = matchedBstopId != null
                    ? busService.getRealtimeArrivals(matchedBstopId)
                    : Collections.emptyList();

            Map<String, Object> busData = new LinkedHashMap<>();
            busData.put("stopName", resolvedStopName);
            busData.put("bstopId", matchedBstopId);
            busData.put("arrivals", arrivals);

            UiComponentDto component = UiComponentDto.of("BUS", busData, "버스 실시간 운행정보", "/bus");

            StringBuilder summary = new StringBuilder();
            summary.append(String.format("[%s] 정류소 실시간 버스 도착 정보입니다.\n", resolvedStopName));
            if (arrivals.isEmpty()) {
                summary.append("현재 운행 대기 중이거나 도착 예정인 버스가 없습니다.");
            } else {
                for (int i = 0; i < Math.min(arrivals.size(), 3); i++) {
                    BusArrivalItemDto item = arrivals.get(i);
                    int arrivalMin = 0;
                    try {
                        if (item.getArrivalEstimateTime() != null) {
                            arrivalMin = Integer.parseInt(item.getArrivalEstimateTime()) / 60;
                        }
                    } catch (Exception ignored) {}
                    summary.append(String.format("• %s: 약 %d분 (%s개 정류소 전)\n", 
                            item.getRouteNo(), arrivalMin, item.getRestStopCount()));
                }
            }

            return new ToolResult(summary.toString().trim(), component, busData);
        } catch (Exception e) {
            log.error("버스 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("버스 도착 정보를 가져오는 데 실패했습니다.", null, null);
        }
    }

    /**
     * 시간표 도구 실행
     */
    public ToolResult executeTimeTable(Member member, Map<String, Object> params) {
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

    /**
     * 학사/학과 일정 도구 실행
     */
    public ToolResult executeSchedule(Member member, Map<String, Object> params) {
        try {
            LocalDate now = LocalDate.now();
            int year = now.getYear();
            int month = now.getMonthValue();

            if (params != null && params.containsKey("year") && params.get("year") != null) {
                try {
                    year = Integer.parseInt(String.valueOf(params.get("year")));
                } catch (NumberFormatException ignored) {}
            }

            if (params != null && params.containsKey("month") && params.get("month") != null) {
                try {
                    int paramMonth = Integer.parseInt(String.valueOf(params.get("month")));
                    if (paramMonth >= 1 && paramMonth <= 12) {
                        month = paramMonth;
                    }
                } catch (NumberFormatException ignored) {}
            }

            List<ScheduleResponseDto> schedules = (member != null)
                    ? scheduleService.getMyDepartmentScheduleByMonth(member, year, month)
                    : scheduleService.getScheduleByMonth(year, month);

            UiComponentDto component = UiComponentDto.of("SCHEDULE", schedules, "학사일정 달력 보기", "/home/calendar");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%d년 %d월 학사일정입니다.\n", year, month));
            if (schedules.isEmpty()) {
                sb.append("해당 월에 예정된 학사 일정이 없습니다.");
            } else {
                for (int i = 0; i < Math.min(schedules.size(), 5); i++) {
                    ScheduleResponseDto s = schedules.get(i);
                    sb.append(String.format("• %s (%s ~ %s)\n", s.getTitle(), s.getStart(), s.getEnd()));
                }
            }

            return new ToolResult(sb.toString().trim(), component, schedules);
        } catch (Exception e) {
            log.error("학사일정 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("학사일정을 조회하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    /**
     * 공지사항 검색 도구 실행
     */
    public ToolResult executeNotice(Map<String, Object> params) {
        try {
            String query = "";
            if (params != null && params.containsKey("query") && params.get("query") != null) {
                query = String.valueOf(params.get("query")).trim();
            }

            ListResponseDto<NoticeListResponseDto> noticeResult;
            if (query.length() >= 2) {
                noticeResult = noticeService.searchNotice(query, null, 1);
            } else {
                List<NoticeListResponseDto> topNotices = noticeService.getTop();
                noticeResult = ListResponseDto.of(topNotices.size(), 1, topNotices);
            }

            List<NoticeListResponseDto> list = noticeResult.getContents() != null 
                    ? noticeResult.getContents() 
                    : Collections.emptyList();

            UiComponentDto component = UiComponentDto.of("NOTICE_LIST", list, "공지사항 전체 목록 보기", "/home/notice");

            StringBuilder sb = new StringBuilder();
            if (query.length() >= 2) {
                sb.append(String.format("'%s' 검색 결과 공지사항 %d건을 찾았습니다.\n", query, noticeResult.getTotal()));
            } else {
                sb.append("최신 학교 공지사항 목록입니다.\n");
            }

            for (int i = 0; i < Math.min(list.size(), 3); i++) {
                NoticeListResponseDto item = list.get(i);
                sb.append(String.format("• [%s] %s (%s)\n", item.getCategory(), item.getTitle(), item.getCreateDate()));
            }

            return new ToolResult(sb.toString().trim(), component, list);
        } catch (Exception e) {
            log.error("공지사항 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("공지사항을 검색하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    /**
     * 교내 행정 및 학과 연락처 도구 실행
     */
    public ToolResult executeDirectory(Map<String, Object> params) {
        try {
            String query = "";
            if (params != null && params.containsKey("query") && params.get("query") != null) {
                query = String.valueOf(params.get("query")).trim();
            }

            ListResponseDto<CollegeOfficeContactResponse> contactResult =
                    collegeOfficeContactService.getContacts(null, query, 1);

            List<CollegeOfficeContactResponse> list = contactResult.getContents() != null
                    ? contactResult.getContents()
                    : Collections.emptyList();

            UiComponentDto component = UiComponentDto.of("DIRECTORY", list, "전화번호부 검색 홈", "/phonebook");

            StringBuilder sb = new StringBuilder();
            if (list.isEmpty()) {
                sb.append(String.format("'%s' 관련 교내 연락처를 찾지 못했습니다. 전화번호부 메뉴에서 직접 검색해보세요.", query));
            } else {
                sb.append(String.format("'%s' 관련 교내 연락처 정보입니다.\n", query));
                for (int i = 0; i < Math.min(list.size(), 3); i++) {
                    CollegeOfficeContactResponse c = list.get(i);
                    sb.append(String.format("• %s (%s): 📞 %s\n", c.getDepartmentName(), c.getCollegeName(), c.getOfficePhoneNumber()));
                }
            }

            return new ToolResult(sb.toString().trim(), component, list);
        } catch (Exception e) {
            log.error("연락처 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("교내 전화번호부를 검색하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    /**
     * 채팅 푸시 알림 설정 액션
     */
    public ToolResult executeActionChatPush(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("채팅 알림 설정을 변경하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        try {
            boolean enabled = true;
            if (params != null && params.containsKey("enabled")) {
                Object val = params.get("enabled");
                if (val instanceof Boolean b) {
                    enabled = b;
                } else {
                    enabled = Boolean.parseBoolean(String.valueOf(val));
                }
            }
            boolean result = memberService.updateChatPush(member.getId(), enabled);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("settingType", "CHAT_PUSH");
            data.put("title", "채팅 푸시 알림");
            data.put("enabled", result);
            data.put("statusText", result ? "알림 켜짐" : "알림 꺼짐");
            data.put("message", result ? "채팅 푸시 알림이 활성화되었습니다." : "채팅 푸시 알림이 비활성화되었습니다.");

            UiComponentDto component = UiComponentDto.of("SETTING_RESULT", data, "내 정보 / 알림 설정", "/my-page");
            String summary = result 
                    ? "채팅 푸시 알림을 성공적으로 켰습니다. 새 메시지가 오면 푸시로 알려드릴게요!"
                    : "채팅 푸시 알림을 성공적으로 껐습니다. 언제든 다시 켜실 수 있어요.";

            return new ToolResult(summary, component, data);
        } catch (Exception e) {
            log.error("채팅 푸시 설정 변경 오류: {}", e.getMessage(), e);
            return new ToolResult("채팅 푸시 알림 설정을 변경하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    /**
     * 데일리 브리프 알림 설정 액션
     */
    public ToolResult executeActionDailyBrief(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("데일리 브리프 설정을 변경하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        try {
            String time = (params != null && params.get("time") != null && !String.valueOf(params.get("time")).isBlank())
                    ? String.valueOf(params.get("time")).trim()
                    : "08:30";

            boolean enabled = true;
            if (params != null && params.containsKey("enabled")) {
                Object val = params.get("enabled");
                if (val instanceof Boolean b) {
                    enabled = b;
                } else {
                    enabled = Boolean.parseBoolean(String.valueOf(val));
                }
            }

            ScheduleScope scope = ScheduleScope.ALL;
            if (params != null && params.get("scope") != null) {
                try {
                    scope = ScheduleScope.valueOf(String.valueOf(params.get("scope")).toUpperCase().trim());
                } catch (Exception ignored) {}
            }

            DailyBriefSettingRequestDto req = new DailyBriefSettingRequestDto(
                    enabled, // timetableAlertEnabled
                    enabled, // timetablePreAlertEnabled
                    10,      // timetablePreAlertMinutes
                    enabled, // timetableDailyBriefEnabled
                    time,    // timetableDailyBriefTime
                    enabled, // scheduleAlertEnabled
                    time,    // scheduleDailyBriefTime
                    scope    // scheduleScope
            );
            dailyBriefService.updateSettings(member, req);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("settingType", "DAILY_BRIEF");
            data.put("title", "데일리 브리프 알림");
            data.put("enabled", enabled);
            data.put("time", time);
            data.put("scope", scope.name());
            data.put("statusText", enabled ? (time + " 발송 예약") : "알림 꺼짐");
            data.put("message", enabled
                    ? String.format("매일 아침 %s에 오늘의 시간표와 학사일정 브리핑을 보내드립니다.", time)
                    : "데일리 브리프 알림이 해제되었습니다.");

            UiComponentDto component = UiComponentDto.of("SETTING_RESULT", data, "브리프 알림 설정", "/home/calendar");
            String summary = enabled
                    ? String.format("데일리 브리프 알림이 매일 아침 %s에 발송되도록 설정되었습니다.", time)
                    : "데일리 브리프 알림이 꺼졌습니다.";

            return new ToolResult(summary, component, data);
        } catch (Exception e) {
            log.error("데일리 브리프 설정 오류: {}", e.getMessage(), e);
            return new ToolResult("데일리 브리프 설정을 변경하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    /**
     * 공지 키워드 알림 등록 액션
     */
    public ToolResult executeActionNoticeKeyword(Member member, Map<String, Object> params) {
        if (member == null) {
            return new ToolResult("공지 키워드 알림을 등록하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        try {
            String keyword = (params != null && params.get("keyword") != null)
                    ? String.valueOf(params.get("keyword")).trim()
                    : "";

            if (keyword.isBlank()) {
                return new ToolResult("등록할 알림 키워드를 찾지 못했습니다. 어떤 키워드로 알림을 등록할지 말씀해주세요.", null, null);
            }

            String targetType = (params != null && params.get("targetType") != null)
                    ? String.valueOf(params.get("targetType")).toUpperCase().trim()
                    : "SCHOOL";

            String category = (params != null && params.get("category") != null)
                    ? String.valueOf(params.get("category")).trim()
                    : "전체";

            boolean isExcluded = false;
            if (params != null && params.containsKey("isExcluded")) {
                Object val = params.get("isExcluded");
                if (val instanceof Boolean b) {
                    isExcluded = b;
                } else {
                    isExcluded = Boolean.parseBoolean(String.valueOf(val));
                }
            }

            Department targetDept = null;
            if ("DEPARTMENT".equalsIgnoreCase(targetType) || "DEPT".equalsIgnoreCase(targetType)) {
                targetDept = member.getDepartment();
            }

            KeywordResponse saved = keywordService.addKeyword(member, keyword, targetDept, category, isExcluded);

            String targetName = (targetDept != null)
                    ? targetDept.getDepartmentName() + " 학과공지"
                    : "학교 전체공지 (" + category + ")";

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", saved.keywordId());
            data.put("keyword", saved.keyword());
            data.put("targetType", (targetDept != null) ? "DEPARTMENT" : "SCHOOL");
            data.put("targetName", targetName);
            data.put("category", category);
            data.put("isExcluded", isExcluded);
            data.put("statusText", isExcluded ? "제외 키워드 등록 완료" : "알림 키워드 등록 완료");

            UiComponentDto component = UiComponentDto.of("KEYWORD_CONFIRM", data, "키워드 알림 목록 관리", "/home/notice");

            String summary = isExcluded
                    ? String.format("[%s] 키워드가 %s의 알림 제외 키워드로 등록되었습니다. 해당 단어가 포함된 공지는 알림에서 제외됩니다.", keyword, targetName)
                    : String.format("'%s' 키워드가 %s 알림으로 등록되었습니다! 새로운 공지가 올라오면 바로 푸시를 보내드릴게요.", keyword, targetName);

            return new ToolResult(summary, component, data);
        } catch (Exception e) {
            log.error("공지 키워드 등록 오류: {}", e.getMessage(), e);
            return new ToolResult("공지 키워드를 등록하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    /**
     * 내 알림 및 설정 상태 조회 액션
     */
    public ToolResult executeActionMySettings(Member member) {
        if (member == null) {
            return new ToolResult("내 알림 설정을 확인하려면 로그인이 필요합니다.",
                    UiComponentDto.of("AUTH_REQUIRED", Map.of(), "로그인하기", "/login"), null);
        }

        try {
            boolean chatPush = Boolean.TRUE.equals(member.getChatPushEnabled());
            DailyBriefSettingResponseDto brief = dailyBriefService.getSettings(member);
            List<KeywordResponse> keywords = keywordService.getKeywords(member);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("chatPushEnabled", chatPush);
            data.put("dailyBrief", brief);
            data.put("keywordCount", keywords.size());
            data.put("keywords", keywords);

            UiComponentDto component = UiComponentDto.of("MY_SETTINGS", data, "설정 페이지 가기", "/my-page");

            StringBuilder sb = new StringBuilder();
            sb.append("현재 회원님의 알림 및 설정 현황입니다:\n");
            sb.append(String.format("• 채팅 푸시 알림: %s\n", chatPush ? "켜짐 🔔" : "꺼짐 🔕"));
            sb.append(String.format("• 데일리 브리프: 시간표(%s, %s) / 학사일정(%s, %s)\n",
                    Boolean.TRUE.equals(brief.timetableDailyBriefEnabled()) ? "켜짐" : "꺼짐",
                    brief.timetableDailyBriefTime() != null ? brief.timetableDailyBriefTime() : "08:00",
                    Boolean.TRUE.equals(brief.scheduleAlertEnabled()) ? "켜짐" : "꺼짐",
                    brief.scheduleDailyBriefTime() != null ? brief.scheduleDailyBriefTime() : "08:30"));
            sb.append(String.format("• 등록된 공지 알림 키워드: 총 %d개", keywords.size()));
            if (!keywords.isEmpty()) {
                sb.append(" (");
                for (int i = 0; i < Math.min(keywords.size(), 3); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(keywords.get(i).keyword());
                }
                if (keywords.size() > 3) sb.append(String.format(" 외 %d개", keywords.size() - 3));
                sb.append(")");
            }

            return new ToolResult(sb.toString().trim(), component, data);
        } catch (Exception e) {
            log.error("알림 설정 조회 오류: {}", e.getMessage(), e);
            return new ToolResult("알림 설정을 조회하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    private String normalizeCafeteriaName(String name) {
        if (name.contains("기숙사") && name.contains("1")) return "제1기숙사식당";
        if (name.contains("기숙사") && name.contains("2")) return "2기숙사 식당";
        if (name.contains("기숙사")) return "제1기숙사식당";
        if (name.contains("2호관") || name.contains("교직원")) return "2호관(교직원)식당";
        if (name.contains("27호관")) return "27호관식당";
        if (name.contains("사범대")) return "사범대식당";
        return "학생식당";
    }

    public record ToolResult(
            String summary,
            UiComponentDto uiComponent,
            Object rawData
    ) {}
}
