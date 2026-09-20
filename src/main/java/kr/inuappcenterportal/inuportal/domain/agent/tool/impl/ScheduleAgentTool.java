package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedule.dto.ScheduleResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleAgentTool implements AgentTool {

    private final ScheduleService scheduleService;

    @Override
    public AgentToolDefinition getDefinition() {
        return new AgentToolDefinition("SCHEDULE", "학교와 소속 학과의 월별 학사일정을 조회합니다.",
                List.of("월별 학사일정 조회", "시험 기간 조회", "수강신청·수강정정 기간 조회", "소속 학과 일정 조회"),
                List.of("이번 달 학사일정 알려줘", "중간고사 기간 언제야?", "다음 달 수강정정 일정 있어?"),
                List.of("절차·자격·학칙 해석은 INU_AI_KNOWLEDGE", "개인 수업 일정은 TIMETABLE"),
                Map.of("year", AgentToolParameter.integer("조회 연도, 미지정 시 현재 연도", false),
                        "month", AgentToolParameter.integer("조회 월 1~12, 미지정 시 현재 월", false)), false, true);
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
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

            List<ScheduleResponseDto> schoolSchedules = scheduleService.getScheduleByMonth(year, month);
            List<ScheduleResponseDto> deptSchedules = (member != null && member.getDepartment() != null)
                    ? scheduleService.getMyDepartmentScheduleByMonth(member, year, month)
                    : Collections.emptyList();

            List<ScheduleResponseDto> allSchedules = new ArrayList<>();
            if (schoolSchedules != null) allSchedules.addAll(schoolSchedules);
            if (deptSchedules != null) allSchedules.addAll(deptSchedules);

            allSchedules.sort(Comparator.comparing(
                    s -> s.getStart() != null ? s.getStart() : "",
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));

            UiComponentDto component = UiComponentDto.of("SCHEDULE", allSchedules, "학사일정 달력 보기", "/home/calendar");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%d년 %d월 학사 및 학과 일정입니다.\n\n", year, month));

            boolean hasContent = false;
            if (schoolSchedules != null && !schoolSchedules.isEmpty()) {
                hasContent = true;
                sb.append("[학교 학사일정]\n");
                for (int i = 0; i < Math.min(schoolSchedules.size(), 4); i++) {
                    ScheduleResponseDto s = schoolSchedules.get(i);
                    sb.append(String.format("• %s: %s ~ %s\n", s.getTitle(), s.getStart(), s.getEnd()));
                }
            }

            if (deptSchedules != null && !deptSchedules.isEmpty()) {
                if (hasContent) sb.append("\n");
                hasContent = true;
                String deptName = (member != null && member.getDepartment() != null)
                        ? member.getDepartment().getDepartmentName()
                        : "학과";
                sb.append(String.format("[%s 학과일정]\n", deptName));
                for (int i = 0; i < Math.min(deptSchedules.size(), 4); i++) {
                    ScheduleResponseDto s = deptSchedules.get(i);
                    sb.append(String.format("• %s: %s ~ %s\n", s.getTitle(), s.getStart(), s.getEnd()));
                }
            }

            if (!hasContent) {
                sb.append("해당 기간에 등록된 주요 학사일정이 없습니다.");
            }

            return new ToolResult(sb.toString().trim(), component, allSchedules);
        } catch (Exception e) {
            log.error("학사일정 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("학사일정을 조회하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("일정") || lower.contains("학사") || lower.contains("시험") || lower.contains("종강") || lower.contains("개강");
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        LocalDate now = LocalDate.now();
        return Map.of("year", now.getYear(), "month", now.getMonthValue());
    }
}
