package kr.inuappcenterportal.inuportal.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "학과 공지 검색 결과 항목")
public class DepartmentNoticeSearchItemDto {
    private Long id;
    private String department;
    private String departmentName;
    private String title;
    private String snippet;
    private String writer;
    private String url;
    private String createDate;
}
