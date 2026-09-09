package kr.inuappcenterportal.inuportal.domain.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentReminderDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.AgentReminderUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.agent.enums.AgentReminderRepeatType;
import kr.inuappcenterportal.inuportal.domain.agent.model.AgentReminder;
import kr.inuappcenterportal.inuportal.domain.agent.repository.AgentReminderRepository;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolRegistry;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class AgentReminderService {

    private final AgentReminderRepository agentReminderRepository;
    private final AgentToolRegistry agentToolRegistry;
    private final FcmService fcmService;
    private final ObjectMapper objectMapper;

    public AgentReminderService(
            AgentReminderRepository agentReminderRepository,
            @Lazy AgentToolRegistry agentToolRegistry,
            FcmService fcmService,
            ObjectMapper objectMapper
    ) {
        this.agentReminderRepository = agentReminderRepository;
        this.agentToolRegistry = agentToolRegistry;
        this.fcmService = fcmService;
        this.objectMapper = objectMapper;
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    public List<AgentReminderDto> getMyReminders(Member member) {
        if (member == null) {
            return Collections.emptyList();
        }
        return agentReminderRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId())
                .stream()
                .map(AgentReminderDto::from)
                .toList();
    }

    @Transactional
    public AgentReminderDto createReminder(
            Member member,
            String title,
            String targetTime,
            AgentReminderRepeatType repeatType,
            String targetTool,
            String toolParamsJson,
            String titleTemplate,
            String bodyTemplate,
            String route
    ) {
        if (member == null) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        // 도구 존재 여부 검증 (정직성 가드레일: 없는 도구 등록 방지)
        String toolKey = targetTool != null ? targetTool.toUpperCase().trim() : "";
        if (agentToolRegistry.findTool(toolKey).isEmpty()) {
            throw new IllegalArgumentException(String.format("지원하지 않는 기능 도구('%s')입니다.", targetTool));
        }

        AgentReminder reminder = AgentReminder.builder()
                .member(member)
                .title(title != null && !title.isBlank() ? title : "AI 맞춤 알림")
                .targetTime(normalizeTime(targetTime))
                .repeatType(repeatType != null ? repeatType : AgentReminderRepeatType.WEEKDAYS)
                .targetTool(toolKey)
                .toolParamsJson(toolParamsJson)
                .titleTemplate(titleTemplate)
                .bodyTemplate(bodyTemplate)
                .route(route != null && !route.isBlank() ? route : "/")
                .enabled(true)
                .build();

        AgentReminder saved = agentReminderRepository.save(reminder);
        log.info("[AgentReminderService] 새 맞춤 알림 등록 완료: id={}, memberId={}, tool={}, time={}",
                saved.getId(), member.getId(), saved.getTargetTool(), saved.getTargetTime());
        return AgentReminderDto.from(saved);
    }

    @Transactional
    public AgentReminderDto updateReminder(Long id, Member member, AgentReminderUpdateRequestDto req) {
        AgentReminder reminder = getMyReminderEntity(id, member);
        String normalizedTime = req.targetTime() != null ? normalizeTime(req.targetTime()) : null;

        reminder.update(
                req.title(),
                normalizedTime,
                req.repeatType(),
                req.toolParamsJson(),
                req.titleTemplate(),
                req.bodyTemplate(),
                req.route(),
                req.enabled()
        );
        return AgentReminderDto.from(reminder);
    }

    @Transactional
    public AgentReminderDto toggleReminder(Long id, Member member, boolean enabled) {
        AgentReminder reminder = getMyReminderEntity(id, member);
        reminder.toggleEnabled(enabled);
        return AgentReminderDto.from(reminder);
    }

    @Transactional
    public void deleteReminder(Long id, Member member) {
        AgentReminder reminder = getMyReminderEntity(id, member);
        agentReminderRepository.delete(reminder);
        log.info("[AgentReminderService] 맞춤 알림 삭제 완료: id={}, memberId={}", id, member.getId());
    }

    /**
     * 스케줄러에서 매 분마다 실행하는 알림 발송 디스패치 메서드
     */
    @Transactional
    public void dispatchDueReminders() {
        LocalDate today = LocalDate.now();
        String currentTimeStr = LocalTime.now().format(TIME_FORMATTER);

        List<AgentReminder> dueReminders = agentReminderRepository.findAllActiveByTargetTime(currentTimeStr);
        if (dueReminders.isEmpty()) {
            return;
        }

        for (AgentReminder reminder : dueReminders) {
            try {
                // 1. 요일 일치 여부 검증
                if (!reminder.getRepeatType().matches(today.getDayOfWeek())) {
                    continue;
                }

                // 2. 당일 중복 발송 방지 (1회성이거나 이미 보낸 경우)
                if (today.equals(reminder.getLastSentDate())) {
                    continue;
                }

                // 3. 대상 도구 파라미터 파싱
                Map<String, Object> params = parseParams(reminder.getToolParamsJson());

                // 4. 대상 도구 실행 (라이브 데이터 획득)
                AgentTool.ToolResult toolResult = agentToolRegistry.execute(
                        reminder.getTargetTool(),
                        reminder.getMember(),
                        params
                );

                // 5. 템플릿 치환 및 메시지 합성 (Zero-LLM: 0ms 문자열 치환)
                String pushTitle = renderTemplate(
                        reminder.getTitleTemplate(),
                        reminder.getTitle(),
                        toolResult.rawData()
                );

                String pushBody = renderBodyTemplate(
                        reminder.getBodyTemplate(),
                        toolResult.summary(),
                        toolResult.rawData()
                );

                // 6. FCM 푸시 전송
                fcmService.sendDailyBriefNotification(
                        reminder.getMember().getId(),
                        pushTitle,
                        pushBody,
                        FcmMessageType.AGENT_CUSTOM_REMINDER,
                        reminder.getRoute()
                );

                // 7. 발송 일자 기록 (1회성이면 자동 비활성화)
                reminder.recordSent(today);

                log.info("[AgentReminderService] 맞춤 알림 발송 성공: reminderId={}, memberId={}, tool={}",
                        reminder.getId(), reminder.getMember().getId(), reminder.getTargetTool());

            } catch (Exception e) {
                log.error("[AgentReminderService] 알림 발송 실패: reminderId={}, error={}",
                        reminder.getId(), e.getMessage(), e);
            }
        }
    }

    private AgentReminder getMyReminderEntity(Long id, Member member) {
        if (member == null) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }
        return agentReminderRepository.findByIdAndMemberId(id, member.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 알림 설정을 찾을 수 없습니다."));
    }

    private Map<String, Object> parseParams(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("[AgentReminderService] toolParamsJson 파싱 실패: {}", json);
            return Collections.emptyMap();
        }
    }

    private String normalizeTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return "08:30";
        String[] parts = timeStr.trim().split(":");
        if (parts.length != 2) return "08:30";
        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            return String.format("%02d:%02d", Math.max(0, Math.min(23, h)), Math.max(0, Math.min(59, m)));
        } catch (Exception e) {
            return "08:30";
        }
    }

    private String renderTemplate(String template, String fallback, Object data) {
        if (template == null || template.isBlank()) {
            return fallback != null && !fallback.isBlank() ? fallback : "🔔 AI 맞춤 알림";
        }
        return substituteVariables(template, data);
    }

    private String renderBodyTemplate(String template, String summaryFallback, Object data) {
        if (template != null && !template.isBlank()) {
            String rendered = substituteVariables(template, data);
            if (!rendered.isBlank()) {
                return rendered;
            }
        }
        if (summaryFallback != null && !summaryFallback.isBlank()) {
            return summaryFallback;
        }
        return "요청하신 알림 정보가 도착했습니다.";
    }

    @SuppressWarnings("unchecked")
    private String substituteVariables(String template, Object data) {
        if (template == null) return "";
        if (data == null) return template;

        String result = template;
        if (data instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = "{" + entry.getKey() + "}";
                String val = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
                result = result.replace(key, val);
            }
        }
        return result;
    }
}
