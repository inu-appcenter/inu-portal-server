package kr.inuappcenterportal.inuportal.domain.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentChatRequestDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentChatResponseDto;
import kr.inuappcenterportal.inuportal.domain.agent.service.AgentService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent")
@Tag(name = "Agent", description = "INTIP AI 캠퍼스 비서 API")
public class AgentController {

    private final AgentService agentService;

    @Operation(
            summary = "AI 에이전트 질의응답 (Generative UI)",
            description = "자연어로 질문하면 의도를 분석하여 학식, 버스, 날씨, 시간표, 학사일정, 공지사항, 교내 연락처 등 적합한 도구를 실행하고, 자연어 요약 답변과 함께 웹에서 렌더링할 UI 컴포넌트 데이터를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "에이전트 답변 생성 성공",
                    content = @Content(schema = @Schema(implementation = AgentChatResponseDto.class))
            )
    })
    @PostMapping("/chat")
    public ResponseEntity<ResponseDto<AgentChatResponseDto>> chat(
            @Valid @RequestBody AgentChatRequestDto requestDto,
            @AuthenticationPrincipal Member member
    ) {
        AgentChatResponseDto response = agentService.processChat(requestDto, member);
        return ResponseEntity.ok(ResponseDto.of(response, "AI 에이전트 응답 성공"));
    }
}
