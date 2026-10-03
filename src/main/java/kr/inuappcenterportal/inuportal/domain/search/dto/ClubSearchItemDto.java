package kr.inuappcenterportal.inuportal.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "동아리 검색 결과 항목")
public class ClubSearchItemDto {
    private Long id;
    private String name;
    private String category;
    private String snippet;
}
