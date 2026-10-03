package kr.inuappcenterportal.inuportal.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "교내 전화번호부 검색 결과 항목")
public class DirectorySearchItemDto {
    private Long id;
    private String name;
    private String affiliation;
    private String detailAffiliation;
    private String position;
    private String duties;
    private String email;
    private String phoneNumber;
}
