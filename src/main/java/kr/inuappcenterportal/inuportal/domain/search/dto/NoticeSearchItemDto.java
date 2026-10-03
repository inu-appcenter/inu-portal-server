package kr.inuappcenterportal.inuportal.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "학교 공지 검색 결과 항목")
public class NoticeSearchItemDto {
    private Long id;
    private String title;
    private String snippet;
    private String writer;
    private String category;
    private String url;
    private String createDate;
}
