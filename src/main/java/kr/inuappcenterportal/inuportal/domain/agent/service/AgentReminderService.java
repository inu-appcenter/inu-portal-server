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
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableService;
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
    private final TimeTableService timeTableService;

    public AgentReminderService(
            AgentReminderRepository agentReminderRepository,
            @Lazy AgentToolRegistry agentToolRegistry,
            FcmService fcmService,
            ObjectMapper objectMapper,
            TimeTableService timeTableService
    ) {
        this.agentReminderRepository = agentReminderRepository;
        this.agentToolRegistry = agentToolRegistry;
        this.fcmService = fcmService;
        this.objectMapper = objectMapper;
        this.timeTableService = timeTableService;
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
            String schedulesJson,
            String titleTemplate,
            String bodyTemplate,
            String route
    ) {
        if (member == null) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        // 도구 존재 여부 검증 (정직성 가드레일: 없는 도구 등록 방지, 쉼표 구분 다중 도구 지원)
        String toolKey = targetTool != null ? targetTool.toUpperCase().trim() : "";
        List<String> tools = Arrays.stream(toolKey.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        if (tools.isEmpty() || tools.stream().anyMatch(t -> agentToolRegistry.findTool(t).isEmpty())) {
            throw new IllegalArgumentException(String.format("지원하지 않는 기능 도구('%s')가 포함되어 있습니다.", targetTool));
        }

        String finalTargetTime = targetTime;
        if ((finalTargetTime == null || finalTargetTime.isBlank()) && schedulesJson != null && !schedulesJson.isBlank()) {
            try {
                List<kr.inuappcenterportal.inuportal.domain.agent.dto.ReminderScheduleDto> schedules = objectMapper.readValue(
                        schedulesJson,
                        new TypeReference<List<kr.inuappcenterportal.inuportal.domain.agent.dto.ReminderScheduleDto>>() {}
                );
                if (schedules != null && !schedules.isEmpty() && schedules.get(0).time() != null) {
                    finalTargetTime = schedules.get(0).time();
                }
            } catch (Exception ignored) {}
        }

        AgentReminder reminder = AgentReminder.builder()
                .member(member)
                .title(title != null && !title.isBlank() ? title : "AI 맞춤 알림")
                .targetTime(normalizeTime(finalTargetTime))
                .repeatType(repeatType != null ? repeatType : AgentReminderRepeatType.WEEKDAYS)
                .targetTool(toolKey)
                .toolParamsJson(toolParamsJson)
                .schedulesJson(schedulesJson)
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
        return createReminder(member, title, targetTime, repeatType, targetTool, toolParamsJson, null, titleTemplate, bodyTemplate, route);
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
                req.schedulesJson(),
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
     * 특정 맞춤 알림을 즉시 테스트 발송합니다.
     */
    @Transactional
    public void testDispatchReminder(Long id, Member member) {
        AgentReminder reminder = getMyReminderEntity(id, member);
        sendReminder(reminder, false, null);
    }

    /**
     * 스케줄러에서 매 분마다 실행하는 알림 발송 디스패치 메서드
     */
     @Transactional
     public void dispatchDueReminders() {
         LocalDate today = LocalDate.now();
         LocalTime now = LocalTime.now();
         String currentTimeStr = now.format(TIME_FORMATTER);

         List<AgentReminder> activeReminders = agentReminderRepository.findAllActive();
         if (activeReminders.isEmpty()) {
             return;
         }

         for (AgentReminder reminder : activeReminders) {
             try {
                 // 1. 다중 스케줄 또는 시간표 기반 동적 조건 일치 여부 검증
                 if (!isReminderDue(reminder, today, now)) {
                     continue;
                 }

                 // 2. 당일 동일 시각 중복 발송 방지 (1회성이거나 해당 시각에 이미 보낸 경우)
                 if (today.equals(reminder.getLastSentDate())) {
                     if (reminder.getRepeatType() == AgentReminderRepeatType.ONCE) {
                         continue;
                     }
                     if (currentTimeStr.equals(reminder.getLastSentTime())) {
                         continue;
                     }
                 }

                 sendReminder(reminder, true, currentTimeStr);

             } catch (Exception e) {
                 log.error("[AgentReminderService] 알림 발송 실패: reminderId={}, error={}",
                         reminder.getId(), e.getMessage(), e);
             }
         }
     }

     /**
      * 해당 알림이 현재 일자 및 시각에 발송되어야 하는지 판단합니다.
      */
     public boolean isReminderDue(AgentReminder reminder, LocalDate today, LocalTime now) {
         if (reminder == null || !reminder.isEnabled()) {
             return false;
         }

         String currentTimeStr = now.format(TIME_FORMATTER);

         // 1. toolParamsJson 내 triggers 검사
         Map<String, Object> params = parseParams(reminder.getToolParamsJson());
         Object triggersObj = params.get("triggers");

         if (triggersObj instanceof List<?> triggerList && !triggerList.isEmpty()) {
             List<TimeTableService.DailyLectureDto> lectures = null;
             boolean dynamicTriggerFound = false;

             for (Object item : triggerList) {
                 if (!(item instanceof Map<?, ?> triggerMap)) continue;
                 String type = triggerMap.get("type") != null ? String.valueOf(triggerMap.get("type")).toUpperCase() : "";

                 switch (type) {
                     case "BEFORE_FIRST_CLASS" -> {
                         dynamicTriggerFound = true;
                         if (lectures == null && reminder.getMember() != null) {
                             lectures = timeTableService.getMemberDailyLectures(reminder.getMember().getId(), today);
                         }
                         if (lectures != null && !lectures.isEmpty()) {
                             int minutes = parseMinutes(triggerMap.get("minutes"), 60);
                             LocalTime firstClassStart = lectures.get(0).startTime();
                             LocalTime triggerTime = firstClassStart.minusMinutes(minutes);
                             if (currentTimeStr.equals(triggerTime.format(TIME_FORMATTER))) {
                                 return true;
                             }
                         }
                     }
                     case "BEFORE_CLASS" -> {
                         dynamicTriggerFound = true;
                         if (lectures == null && reminder.getMember() != null) {
                             lectures = timeTableService.getMemberDailyLectures(reminder.getMember().getId(), today);
                         }
                         if (lectures != null && !lectures.isEmpty()) {
                             int minutes = parseMinutes(triggerMap.get("minutes"), 10);
                             for (TimeTableService.DailyLectureDto lec : lectures) {
                                 LocalTime triggerTime = lec.startTime().minusMinutes(minutes);
                                 if (currentTimeStr.equals(triggerTime.format(TIME_FORMATTER))) {
                                     return true;
                                 }
                             }
                         }
                     }
                     case "AFTER_LAST_CLASS" -> {
                         dynamicTriggerFound = true;
                         if (lectures == null && reminder.getMember() != null) {
                             lectures = timeTableService.getMemberDailyLectures(reminder.getMember().getId(), today);
                         }
                         if (lectures != null && !lectures.isEmpty()) {
                             int offsetMinutes = parseMinutes(triggerMap.get("offsetMinutes"), 10);
                             LocalTime lastClassEnd = lectures.get(lectures.size() - 1).endTime();
                             LocalTime triggerTime = lastClassEnd.plusMinutes(offsetMinutes);
                             if (currentTimeStr.equals(triggerTime.format(TIME_FORMATTER))) {
                                 return true;
                             }
                         }
                     }
                     case "LONG_BREAK" -> {
                         dynamicTriggerFound = true;
                         if (lectures == null && reminder.getMember() != null) {
                             lectures = timeTableService.getMemberDailyLectures(reminder.getMember().getId(), today);
                         }
                         if (lectures != null && lectures.size() >= 2) {
                             int minGapMinutes = parseMinutes(triggerMap.get("minGapMinutes"), 120);
                             for (int i = 0; i < lectures.size() - 1; i++) {
                                 LocalTime endPrev = lectures.get(i).endTime();
                                 LocalTime startNext = lectures.get(i + 1).startTime();
                                 long gap = java.time.Duration.between(endPrev, startNext).toMinutes();
                                 if (gap >= minGapMinutes) {
                                     if (currentTimeStr.equals(endPrev.format(TIME_FORMATTER))) {
                                         return true;
                                     }
                                 }
                             }
                         }
                     }
                     case "NO_CLASS_DAY" -> {
                         dynamicTriggerFound = true;
                         if (lectures == null && reminder.getMember() != null) {
                             lectures = timeTableService.getMemberDailyLectures(reminder.getMember().getId(), today);
                         }
                         if (lectures != null && lectures.isEmpty()) {
                             String checkTime = triggerMap.get("time") != null ? normalizeTime(String.valueOf(triggerMap.get("time"))) : "10:00";
                             if (currentTimeStr.equals(checkTime)) {
                                 return true;
                             }
                         }
                     }
                     case "TIME" -> {
                         dynamicTriggerFound = true;
                         String checkTime = triggerMap.get("time") != null ? normalizeTime(String.valueOf(triggerMap.get("time"))) : reminder.getTargetTime();
                         if (currentTimeStr.equals(checkTime)) {
                             return true;
                         }
                     }
                 }
             }

             if (dynamicTriggerFound) {
                 return false;
             }
         }

         return reminder.matchesSchedule(today, now, objectMapper);
     }

     private int parseMinutes(Object obj, int defaultVal) {
         if (obj == null) return defaultVal;
         try {
             return Integer.parseInt(String.valueOf(obj).trim());
         } catch (Exception e) {
             return defaultVal;
         }
     }

    private void sendReminder(AgentReminder reminder, boolean recordDate, String sentTime) {
        LocalDate today = LocalDate.now();
        String targetTool = reminder.getTargetTool();
        Map<String, Object> params = parseParams(reminder.getToolParamsJson());

        List<String> toolNames = new ArrayList<>();
        if (targetTool != null && targetTool.contains(",")) {
            toolNames.addAll(Arrays.stream(targetTool.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList());
        } else if (targetTool != null && !targetTool.isBlank()) {
            toolNames.add(targetTool.trim());
        }

        List<String> formattedSummaries = new ArrayList<>();
        Object primaryRawData = null;

        for (String tName : toolNames) {
            try {
                AgentTool.ToolResult toolResult = agentToolRegistry.execute(
                        tName,
                        reminder.getMember(),
                        params
                );

                if (toolResult != null) {
                    if (primaryRawData == null) {
                        primaryRawData = toolResult.rawData();
                    }

                    Optional<AgentTool> toolOpt = agentToolRegistry.findTool(tName);
                    String line = toolOpt.map(t -> t.formatNotification(toolResult, params))
                            .orElse(toolResult.summary());
                    if (line != null && !line.isBlank()) {
                        formattedSummaries.add(line);
                    }
                }
            } catch (Exception e) {
                log.warn("[AgentReminderService] 개별 도구({}) 실행 오류: {}", tName, e.getMessage());
            }
        }

        String autoSummary = String.join("\n", formattedSummaries);

        // 템플릿 치환
        String pushTitle = renderTemplate(
                reminder.getTitleTemplate(),
                reminder.getTitle(),
                primaryRawData
        );

        String pushBody = renderBodyTemplate(
                reminder.getBodyTemplate(),
                autoSummary,
                primaryRawData
        );

        // FCM 푸시 전송
        fcmService.sendDailyBriefNotification(
                reminder.getMember().getId(),
                pushTitle,
                pushBody,
                FcmMessageType.AGENT_CUSTOM_REMINDER,
                reminder.getRoute()
        );

        if (recordDate) {
            reminder.recordSent(today, sentTime);
        }

        log.info("[AgentReminderService] 맞춤 알림 발송 완료: reminderId={}, memberId={}, tool={}, title={}",
                reminder.getId(), reminder.getMember().getId(), reminder.getTargetTool(), pushTitle);
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
        String rendered = substituteVariables(template, data);
        return (!rendered.isBlank() && !rendered.contains("{")) ? rendered : (fallback != null ? fallback : "🔔 AI 맞춤 알림");
    }

    private String renderBodyTemplate(String template, String autoSummary, Object data) {
        if (template != null && !template.isBlank()) {
            String rendered = substituteVariables(template, data);
            // 만약 미치환된 변수({xxx})가 남아있지 않고 정상 렌더링되었으면 사용
            if (!rendered.isBlank() && !rendered.contains("{")) {
                return rendered;
            }
        }
        if (autoSummary != null && !autoSummary.isBlank()) {
            return autoSummary;
        }
        return "요청하신 캠퍼스 정보가 도착했습니다.";
    }

    @SuppressWarnings("unchecked")
    private String substituteVariables(String template, Object data) {
        if (template == null) return "";
        if (data == null) return template;

        Map<String, Object> map;
        if (data instanceof Map<?, ?> m) {
            map = (Map<String, Object>) m;
        } else {
            try {
                map = objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                return template;
            }
        }

        String result = template;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String val = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result = result.replace(key, val);
        }
        return result;
    }
}
