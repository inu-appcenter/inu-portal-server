package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 인천대학교 학산도서관 열람실 좌석 및 스터디룸/공간 예약 Agent Tool (Client-Side Action 연동)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryAgentTool implements AgentTool {

    @Override
    public String getName() {
        return "LIBRARY";
    }

    @Override
    public String getDescription() {
        return "인천대학교 학산도서관 열람실 실시간 잔여 좌석(제1~3열람실, 노트북실, 라운지 등), 좌석 배정 및 예약, 좌석 연장, 좌석 반납(퇴실), 스터디룸 및 세미나실 공간 조회/예약 관련 질문 (params: {\"target\": \"SEATS\"|\"STUDY_ROOMS\"|\"MY_SEAT\"|\"RESERVE\"|\"RENEW\"|\"RETURN\", \"roomName\": \"제1열람실\"|\"제1노트북실\" 등})";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        String target = "SEATS";
        String roomName = "";

        if (params != null) {
            if (params.containsKey("target") && params.get("target") != null) {
                target = String.valueOf(params.get("target")).toUpperCase();
            }
            if (params.containsKey("roomName") && params.get("roomName") != null) {
                roomName = String.valueOf(params.get("roomName")).trim();
            }
        }

        log.info("[LibraryAgentTool] execute target: {}, roomName: {}", target, roomName);

        // 클라이언트 단말에서 안전하게 도서관 API(pyxis-api)를 직접 호출할 수 있도록 액션 카드와 명령 전달
        UiComponentDto ui = UiComponentDto.of(
                "LIBRARY_ACTION",
                Map.of(
                        "target", target,
                        "roomName", roomName,
                        "message", "학산도서관 실시간 좌석 및 공간 정보 연동"
                ),
                "도서관 바로가기",
                "/library"
        );

        String summary = String.format("학산도서관 %s 정보를 조회하기 위해 도서관 연동 세션을 연결합니다. 모바일 앱 환경에서 실시간 좌석 및 예약 현황이 직접 조회됩니다.",
                "STUDY_ROOMS".equalsIgnoreCase(target) ? "스터디룸/공간" : (!roomName.isBlank() ? roomName : "열람실 잔여 좌석"));

        return new ToolResult(summary, ui, Map.of(
                "clientAction", "EXECUTE_LIBRARY_ACTION",
                "target", target,
                "roomName", roomName
        ));
    }
}
