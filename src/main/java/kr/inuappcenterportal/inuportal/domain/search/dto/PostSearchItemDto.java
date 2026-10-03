package kr.inuappcenterportal.inuportal.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "게시글 검색 결과 항목")
public class PostSearchItemDto {
    private Long id;
    private String title;
    private String snippet;
    private String category;
    private String writer;
    private Integer good;
    private Integer scrap;
    private String createDate;
}
