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
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableService;
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

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("hasTimetable", primary != null);
            data.put("timetable", primary);
            data.put("allTimetables", timeTables);

            UiComponentDto component = UiComponentDto.of("TIMETABLE", data, "내 시간표 전체보기", "/timetable");
            
            String summary = primary != null
                    ? String.format("'%s' 대표 시간표가 조회되었습니다. 시간표 페이지에서 상세 강의 일정을 확인하실 수 있습니다.", primary.timeTableName())
                    : "등록된 시간표가 없습니다. 시간표 메뉴에서 새 시간표를 등록해보세요.";

            return new ToolResult(summary, component, data);
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
