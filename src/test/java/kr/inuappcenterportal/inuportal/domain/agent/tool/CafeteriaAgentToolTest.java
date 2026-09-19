package kr.inuappcenterportal.inuportal.domain.agent.tool;

import kr.inuappcenterportal.inuportal.domain.agent.tool.impl.CafeteriaAgentTool;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CafeteriaAgentToolTest {

    @Mock
    private CafeteriaService cafeteriaService;

    @InjectMocks
    private CafeteriaAgentTool cafeteriaAgentTool;

    @Test
    @DisplayName("단일 식당 알림 포맷 시 첫 번째 메인메뉴만 깔끔하게 추출된다")
    void formatNotificationSingleCafeteriaExtractsFirstMainMenu() {
        Map<String, Object> data = Map.of(
                "isAllCafeterias", false,
                "cafeteria", "학생식당",
                "targetMeal", "중식",
                "lunch", """
                        [1코너(백반)]
                        참치김치찌개
                        쌀밥
                        계란말이
                        배추김치

                        [2코너(일품)]
                        치즈돈까스
                        우동"""
        );
        AgentTool.ToolResult result = new AgentTool.ToolResult("요약", null, data);

        String notification = cafeteriaAgentTool.formatNotification(result, Map.of());

        assertEquals("[학생식당] 참치김치찌개", notification);
    }

    @Test
    @DisplayName("선택형 및 원산지/가격 표기가 포함된 메뉴에서도 첫 번째 메인 요리만 추출된다")
    void formatNotificationHandlesChoiceAndParenthesis() {
        Map<String, Object> data = Map.of(
                "isAllCafeterias", false,
                "cafeteria", "2호관(교직원)식당",
                "targetMeal", "중식",
                "lunch", """
                        [선택1] 제육볶음(pork), 콩나물국
                        [선택2] 닭칼국수

                        [공통]
                        해물완자전*케찹
                        8,000원(구성원 7,000원)
                        1,522/1,871kcal"""
        );
        AgentTool.ToolResult result = new AgentTool.ToolResult("요약", null, data);

        String notification = cafeteriaAgentTool.formatNotification(result, Map.of());

        assertEquals("[2호관식당] 제육볶음", notification);
    }

    @Test
    @DisplayName("전체 식당 알림 시 각 식당의 첫 번째 메인메뉴가 [식당명1] 메인메뉴, [식당명2] 메인메뉴 형태로 구성된다")
    void formatNotificationAllCafeteriasExtractsEachFirstMainMenu() {
        List<Map<String, Object>> cafeterias = List.of(
                Map.of(
                        "name", "학생식당",
                        "isOperated", true,
                        "menu", "[1코너(백반)]\n참치김치찌개\n쌀밥\n계란말이"
                ),
                Map.of(
                        "name", "2호관(교직원)식당",
                        "isOperated", true,
                        "menu", "[선택1] 제육볶음(pork), 콩나물국\n[선택2] 닭칼국수"
                ),
                Map.of(
                        "name", "제1기숙사식당",
                        "isOperated", true,
                        "menu", "등심돈까스\n우동국물\n단무지"
                ),
                Map.of(
                        "name", "27호관식당",
                        "isOperated", true,
                        "menu", "[비빔밥·돈가스]\n제육야채비빔밥 8,500원(구성원 7,500원)\n주꾸미비빔밥 8,500원"
                ),
                Map.of(
                        "name", "사범대식당",
                        "isOperated", false,
                        "menu", "-"
                )
        );

        Map<String, Object> data = Map.of(
                "isAllCafeterias", true,
                "targetMeal", "중식",
                "cafeterias", cafeterias
        );
        AgentTool.ToolResult result = new AgentTool.ToolResult("요약", null, data);

        String notification = cafeteriaAgentTool.formatNotification(result, Map.of());

        assertEquals("[학생식당] 참치김치찌개, [2호관식당] 제육볶음, [기숙사식당] 등심돈까스, [27호관식당] 제육야채비빔밥", notification);
    }

    @Test
    @DisplayName("actions 리스트 내 특정 식당이 설정되어 있으면 해당 단일 식당만 조회되어 결과가 생성된다")
    void executeWithSpecificCafeteriaInActionsOnlyQueriesThatCafeteria() {
        org.mockito.BDDMockito.given(cafeteriaService.getCafeteria(org.mockito.ArgumentMatchers.eq("제1기숙사식당"), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(List.of("-", "등심돈까스\n우동국물", "-"));

        Map<String, Object> params = Map.of(
                "actions", List.of(
                        Map.of(
                                "type", "CAFETERIA",
                                "cafeteriaParams", Map.of(
                                        "restaurant", "제1기숙사식당",
                                        "mealType", "LUNCH"
                                )
                        )
                )
        );

        AgentTool.ToolResult result = cafeteriaAgentTool.execute(null, params);

        org.junit.jupiter.api.Assertions.assertNotNull(result);
        String notif = cafeteriaAgentTool.formatNotification(result, params);
        assertEquals("[기숙사식당] 등심돈까스", notif);
    }
}
