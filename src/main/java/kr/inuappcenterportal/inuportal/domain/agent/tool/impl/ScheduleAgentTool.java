package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedule.dto.ScheduleResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleAgentTool implements AgentTool {

    private final ScheduleService scheduleService;

    @Override
    public String getName() {
        return "SCHEDULE";
    }

    @Override
    public String getDescription() {
        return "학사일정, 시험기간, 수강신청/정정 기간, 학과 일정 관련 질문 (params: {\"year\": YYYY, \"month\": MM})";
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

            List<ScheduleResponseDto> schedules = (member != null)
                    ? scheduleService.getMyDepartmentScheduleByMonth(member, year, month)
                    : scheduleService.getScheduleByMonth(year, month);

            UiComponentDto component = UiComponentDto.of("SCHEDULE", schedules, "학사일정 달력 보기", "/home/calendar");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%d년 %d월 학사일정입니다.\n", year, month));
            if (schedules.isEmpty()) {
                sb.append("해당 기간에 등록된 주요 학사일정이 없습니다.");
            } else {
                for (int i = 0; i < Math.min(schedules.size(), 4); i++) {
                    ScheduleResponseDto s = schedules.get(i);
                    sb.append(String.format("• %s: %s ~ %s\n", s.getTitle(), s.getStart(), s.getEnd()));
                }
            }

            return new ToolResult(sb.toString().trim(), component, schedules);
        } catch (Exception e) {
            log.error("학사일정 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("학사일정을 조회하는 도중 오류가 발생했습니다.", null, null);
        }
    }
}
