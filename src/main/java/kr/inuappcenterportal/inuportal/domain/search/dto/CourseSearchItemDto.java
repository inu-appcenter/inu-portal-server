package kr.inuappcenterportal.inuportal.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "개설 강의 검색 결과 항목")
public class CourseSearchItemDto {
    private Long id;
    private String subjectNumber;
    private String title;
    private String englishTitle;
    private String professor;
    private Integer credit;
    private String hyName;
    private String isuName;
}
