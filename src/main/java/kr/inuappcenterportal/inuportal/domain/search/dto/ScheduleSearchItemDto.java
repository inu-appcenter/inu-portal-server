package kr.inuappcenterportal.inuportal.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "학사일정 검색 결과 항목")
public class ScheduleSearchItemDto {
    private Long id;
    private String content;
    private String startDate;
    private String endDate;
    private String department;
    private Boolean aiGenerated;
}
