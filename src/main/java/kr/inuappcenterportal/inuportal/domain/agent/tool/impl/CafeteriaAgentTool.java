package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CafeteriaAgentTool implements AgentTool {

    private final CafeteriaService cafeteriaService;

    private static final List<String> ALL_CAFETERIAS = List.of(
            "학생식당", "제1기숙사식당", "27호관식당", "2호관(교직원)식당", "사범대식당", "2기숙사 식당"
    );

    @Override
    public String getName() {
        return "CAFETERIA";
    }

    @Override
    public String getDescription() {
        return "학식, 식당, 메뉴, 밥, 점심, 저녁, 고기 메뉴, 메뉴 추천 관련 질문 (params: {\"cafeteria\": \"전체\"|\"학생식당\"|\"제1기숙사식당\"|\"2기숙사 식당\"|\"2호관(교직원)식당\"|\"27호관식당\"|\"사범대식당\", \"mealType\": \"AUTO\"|\"BREAKFAST\"|\"LUNCH\"|\"DINNER\", \"day\": 요일(1=월~7=일)}). 특정 식당 언급이 없으면 반드시 cafeteria는 \"전체\"로 설정하세요.";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
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

            int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
            if (params != null && params.containsKey("day") && params.get("day") != null) {
                try {
                    dayOfWeek = Integer.parseInt(String.valueOf(params.get("day")));
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
                // 심야/새벽(08:00 이전)은 조식 미운영 식당이 많고 학생들의 주 관심사가 당일 점심이므로 기본 중식으로 매칭
                if (now.isBefore(LocalTime.of(8, 0))) {
                    slotIndex = 1;
                    mealName = "중식(점심)";
                } else if (now.isBefore(LocalTime.of(10, 0))) {
                    slotIndex = 0;
                    mealName = "조식(아침)";
                } else if (now.isBefore(LocalTime.of(14, 30))) {
                    slotIndex = 1;
                    mealName = "중식(점심)";
                } else {
                    slotIndex = 2;
                    mealName = "석식(저녁)";
                }
            }

            if (isAll) {
                // 아침 시간대(08:00~10:00)라도 조식 운영 식당이 전혀 없으면 중식으로 자동 폴백 검사
                if (slotIndex == 0 && "AUTO".equals(mealTypeParam)) {
                    boolean anyBreakfastOperated = false;
                    for (String name : ALL_CAFETERIAS) {
                        List<String> menus = cafeteriaService.getCafeteria(name, dayOfWeek);
                        if (!menus.isEmpty()) {
                            String m = menus.get(0);
                            if (!"-".equals(m) && !m.isBlank() && !m.contains("쉬는 날") && !m.contains("오늘은 쉽니다")) {
                                anyBreakfastOperated = true;
                                break;
                            }
                        }
                    }
                    if (!anyBreakfastOperated) {
                        slotIndex = 1;
                        mealName = "중식(점심)";
                    }
                }

                List<Map<String, Object>> cafeteriaList = new ArrayList<>();
                StringBuilder summary = new StringBuilder();
                summary.append(String.format("[현재 시간대 기준: %s] 전 캠퍼스 식당 메뉴 현황:\n", mealName));

                String firstOperatedCafeteria = null;

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
                        if (firstOperatedCafeteria == null) {
                            firstOperatedCafeteria = name;
                        }
                        String cleanMenu = menu.replaceAll("\\n+", " | ").trim();
                        summary.append(String.format("• [%s]: %s\n", name, cleanMenu));
                    }
                }

                // 만약 현재 끼니에 운영하는 식당이 전혀 없으면 학생식당을 기본값으로 유지
                if (firstOperatedCafeteria == null) {
                    firstOperatedCafeteria = "학생식당";
                }

                Map<String, Object> cafeteriaData = new LinkedHashMap<>();
                cafeteriaData.put("isAllCafeterias", true);
                cafeteriaData.put("cafeteria", firstOperatedCafeteria);
                cafeteriaData.put("targetMeal", slotIndex == 0 ? "조식" : (slotIndex == 2 ? "석식" : "중식"));
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
                cafeteriaData.put("targetMeal", slotIndex == 0 ? "조식" : (slotIndex == 2 ? "석식" : "중식"));
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

    private String normalizeCafeteriaName(String name) {
        if (name.contains("기숙사") && name.contains("1")) return "제1기숙사식당";
        if (name.contains("기숙사") && name.contains("2")) return "2기숙사 식당";
        if (name.contains("기숙사")) return "제1기숙사식당";
        if (name.contains("2호관") || name.contains("교직원")) return "2호관(교직원)식당";
        if (name.contains("27호관")) return "27호관식당";
        if (name.contains("사범대")) return "사범대식당";
        return "학생식당";
    }
}
