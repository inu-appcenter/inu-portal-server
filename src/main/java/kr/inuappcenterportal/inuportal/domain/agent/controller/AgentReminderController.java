package kr.inuappcenterportal.inuportal.domain.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentReminderDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentReminderUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.agent.service.AgentReminderService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Agent Reminder", description = "AI 맞춤 알림(동적 스케줄링) 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent/reminders")
public class AgentReminderController {

    private final AgentReminderService agentReminderService;

    @Operation(summary = "내 맞춤 알림 목록 조회", description = "로그인한 사용자의 AI 맞춤 알림 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ResponseDto<List<AgentReminderDto>>> getMyReminders(
            @AuthenticationPrincipal Member member
    ) {
        List<AgentReminderDto> list = agentReminderService.getMyReminders(member);
        return ResponseEntity.ok(ResponseDto.of(list, "맞춤 알림 목록 조회 성공"));
    }

    @Operation(summary = "맞춤 알림 수정", description = "시간, On/Off 활성화, 제목 등의 알림 설정을 수정합니다.")
    @PatchMapping("/{id}")
    public ResponseEntity<ResponseDto<AgentReminderDto>> updateReminder(
            @PathVariable Long id,
            @AuthenticationPrincipal Member member,
            @RequestBody AgentReminderUpdateRequestDto req
    ) {
        AgentReminderDto updated = agentReminderService.updateReminder(id, member, req);
        return ResponseEntity.ok(ResponseDto.of(updated, "맞춤 알림 수정 성공"));
    }

    @Operation(summary = "맞춤 알림 활성/비활성 토글", description = "알림의 On/Off 상태를 토글합니다.")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ResponseDto<AgentReminderDto>> toggleReminder(
            @PathVariable Long id,
            @AuthenticationPrincipal Member member,
            @RequestParam boolean enabled
    ) {
        AgentReminderDto updated = agentReminderService.toggleReminder(id, member, enabled);
        return ResponseEntity.ok(ResponseDto.of(updated, "맞춤 알림 상태 변경 성공"));
    }

    @Operation(summary = "맞춤 알림 생성", description = "새로운 맞춤 알림/루틴을 등록합니다.")
    @PostMapping
    public ResponseEntity<ResponseDto<AgentReminderDto>> createReminder(
            @AuthenticationPrincipal Member member,
            @RequestBody kr.inuappcenterportal.inuportal.domain.agent.dto.AgentReminderCreateRequestDto req
    ) {
        AgentReminderDto created = agentReminderService.createReminder(
                member,
                req.title(),
                req.targetTime(),
                req.repeatType(),
                req.targetTool(),
                req.toolParamsJson(),
                req.schedulesJson(),
                req.titleTemplate(),
                req.bodyTemplate(),
                req.route()
        );
        return ResponseEntity.ok(ResponseDto.of(created, "맞춤 알림 생성 성공"));
    }

    @Operation(summary = "맞춤 알림 즉시 테스트 발송", description = "특정 맞춤 알림을 현재 시각에 즉시 FCM으로 테스트 발송합니다.")
    @PostMapping("/{id}/test")
    public ResponseEntity<ResponseDto<Void>> testDispatchReminder(
            @PathVariable Long id,
            @AuthenticationPrincipal Member member
    ) {
        agentReminderService.testDispatchReminder(id, member);
        return ResponseEntity.ok(ResponseDto.of(null, "맞춤 알림 테스트 발송 성공"));
    }

    @Operation(summary = "기본/맞춤 알림 즉시 테스트 발송", description = "도구와 파라미터를 기반으로 즉시 FCM 테스트 발송을 수행합니다 (기본 루틴 및 미등록 루틴 지원).")
    @PostMapping("/test-custom")
    public ResponseEntity<ResponseDto<Void>> testDispatchCustom(
            @AuthenticationPrincipal Member member,
            @RequestBody kr.inuappcenterportal.inuportal.domain.agent.dto.AgentReminderTestRequestDto req
    ) {
        agentReminderService.testDispatchCustom(
                member,
                req.title(),
                req.targetTool(),
                req.toolParamsJson(),
                req.titleTemplate(),
                req.bodyTemplate(),
                req.route()
        );
        return ResponseEntity.ok(ResponseDto.of(null, "테스트 알림 발송 성공"));
    }

    @Operation(summary = "맞춤 알림 삭제", description = "특정 맞춤 알림을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteReminder(
            @PathVariable Long id,
            @AuthenticationPrincipal Member member
    ) {
        agentReminderService.deleteReminder(id, member);
        return ResponseEntity.ok(ResponseDto.of(null, "맞춤 알림 삭제 성공"));
    }
}
