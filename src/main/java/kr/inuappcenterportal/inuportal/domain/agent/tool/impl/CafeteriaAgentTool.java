package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
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
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("CAFETERIA", "교내 식당의 요일·식사 시간대별 메뉴를 조회하고 추천합니다.",
                List.of("전체 또는 특정 식당 메뉴 조회", "조식·중식·석식 메뉴 조회", "메뉴 조건 기반 추천"),
                List.of("오늘 점심 뭐야?", "기숙사 저녁 메뉴 알려줘", "고기 나오는 식당 추천해줘"),
                List.of("특정 시각에 학식을 알려달라는 예약 요청은 ACTION_MANAGE_REMINDER"),
                Map.of("cafeteria", AgentToolParameter.string("미지정 시 전체", false, "전체", "학생식당", "제1기숙사식당", "2기숙사 식당", "2호관(교직원)식당", "27호관식당", "사범대식당"),
                        "mealType", AgentToolParameter.string("식사 구분", false, "AUTO", "BREAKFAST", "LUNCH", "DINNER"),
                        "day", AgentToolParameter.integer("요일, 월요일=1부터 일요일=7", false)), false, true);
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

    private String simplifyCafeteriaName(String name) {
        if (name == null) return "식당";
        if (name.contains("학생")) return "학생";
        if (name.contains("2호관") || name.contains("교직원")) return "2호관";
        if (name.contains("기숙사") || name.contains("1기숙사")) return "기숙사";
        if (name.contains("27호관")) return "27호관";
        if (name.contains("사범대")) return "사범대";
        return name.replace("식당", "");
    }

    private String extractFirstMainMenu(String rawMenu) {
        if (rawMenu == null || rawMenu.isBlank() || "-".equals(rawMenu.trim())) {
            return null;
        }
        if (rawMenu.contains("쉬는 날") || rawMenu.contains("오늘은 쉽니다") || rawMenu.contains("등록된 메뉴가 없습니다")) {
            return null;
        }

        String[] lines = rawMenu.split("[\\r\\n]+");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // [1코너], [공통], [비빔밥·돈가스] 등 대괄호로만 이루어진 헤더 라인은 건너뜀
            if (trimmed.matches("^\\[[^\\]]+\\]$")) {
                continue;
            }

            // 가격 / 칼로리만 있는 라인 건너뜀
            if (trimmed.matches("^\\s*\\d{1,3}(,\\d{3})*(\\s*원|\\s*kcal).*") || trimmed.matches(".*(kcal|Kcal|KCAL)$")) {
                continue;
            }

            // "[선택1] 제육볶음" 같은 경우 앞의 태그 제거
            String cleaned = trimmed.replaceAll("^\\[[^\\]]+\\]\\s*", "");

            // 가격 표기 제거 (예: "8,500원(구성원 7,500원)", "7,500원" 등)
            cleaned = cleaned.replaceAll("\\d{1,3}(,\\d{3})*\\s*원.*", "");
            cleaned = cleaned.replaceAll("\\(구성원.*\\)", "");
            cleaned = cleaned.replaceAll("\\d+([,\\.]\\d+)?\\s*(kcal|Kcal)", "");

            // 괄호 안의 부가 설명이나 원산지 제거 (예: "(pork)", "(비엔나/미니해쉬)")
            cleaned = cleaned.replaceAll("\\([^)]*\\)", "");

            // ", 콩나물국", "&소면", "/ 순대국밥", "*새우튀김" 등 첫 번째 메인 요리 뒤에 붙는 국/사이드/선택지 정리
            if (cleaned.contains(",")) {
                cleaned = cleaned.split(",")[0];
            }
            if (cleaned.contains("&")) {
                cleaned = cleaned.split("&")[0];
            }
            if (cleaned.contains("/")) {
                cleaned = cleaned.split("/")[0];
            }
            if (cleaned.contains("*")) {
                cleaned = cleaned.split("\\*")[0];
            }

            cleaned = cleaned.trim();
            if (!cleaned.isEmpty() && !"-".equals(cleaned)) {
                if (cleaned.length() > 20) {
                    cleaned = cleaned.substring(0, 20).trim();
                }
                return cleaned;
            }
        }
        return null;
    }

    @Override
    public String formatNotification(ToolResult result, Map<String, Object> params) {
        if (result == null || !(result.rawData() instanceof Map<?, ?> data)) {
            return result != null && result.summary() != null ? result.summary() : "오늘의 학식 정보입니다.";
        }

        boolean isAll = Boolean.TRUE.equals(data.get("isAllCafeterias"));
        String targetMeal = data.get("targetMeal") != null ? String.valueOf(data.get("targetMeal")) : "중식";

        if (!isAll) {
            String cafName = data.get("cafeteria") != null ? String.valueOf(data.get("cafeteria")) : "학생식당";
            String menu = switch (targetMeal) {
                case "조식" -> data.get("breakfast") != null ? String.valueOf(data.get("breakfast")) : "-";
                case "석식" -> data.get("dinner") != null ? String.valueOf(data.get("dinner")) : "-";
                default -> data.get("lunch") != null ? String.valueOf(data.get("lunch")) : "-";
            };
            String firstMenu = extractFirstMainMenu(menu);
            if (firstMenu == null || firstMenu.isBlank()) {
                return String.format("🍱 [%s %s] 오늘은 식당 운영이 없습니다.", cafName, targetMeal);
            }
            return String.format("🍱 [%s %s] %s", cafName, targetMeal, firstMenu);
        } else {
            Object rawList = data.get("cafeterias");
            if (rawList instanceof List<?> list && !list.isEmpty()) {
                List<String> operatedSummaries = new ArrayList<>();
                for (Object itemObj : list) {
                    if (itemObj instanceof Map<?, ?> itemMap) {
                        boolean isOp = Boolean.TRUE.equals(itemMap.get("isOperated"));
                        if (isOp) {
                            String name = String.valueOf(itemMap.get("name"));
                            String rawMenu = String.valueOf(itemMap.get("menu"));
                            String firstMenu = extractFirstMainMenu(rawMenu);
                            if (firstMenu != null && !firstMenu.isBlank()) {
                                operatedSummaries.add(String.format("%s(%s)", simplifyCafeteriaName(name), firstMenu));
                            }
                        }
                    }
                }
                if (!operatedSummaries.isEmpty()) {
                    return String.format("🍱 [학식 %s] %s", targetMeal, String.join(", ", operatedSummaries));
                }
            }
            return String.format("🍱 [캠퍼스 학식 %s] 운영 중인 식당 메뉴를 확인해 보세요.", targetMeal);
        }
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("학식") || lower.contains("메뉴") || lower.contains("식당") || lower.contains("밥") || lower.contains("점심") || lower.contains("저녁");
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        return Map.of("cafeteria", "전체", "mealType", "AUTO");
    }
}
