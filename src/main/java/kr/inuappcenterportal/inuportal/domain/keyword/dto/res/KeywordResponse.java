package kr.inuappcenterportal.inuportal.domain.keyword.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.keyword.domain.Keyword;
import kr.inuappcenterportal.inuportal.domain.keyword.enums.KeywordCategory;

@Schema(description = "키워드 알림 생성 응답")
public record KeywordResponse(

        @Schema(description = "회원 Id", example = "1")
        Long memberId,

        @Schema(description = "키워드", example = "수강신청")
        String keyword,

        @Schema(description = "알림 카테고리")
        KeywordCategory keywordCategory

) {
    public static KeywordResponse from(Keyword keyword) {
        return new KeywordResponse(keyword.getMemberId(), keyword.getKeyword(), keyword.getKeywordCategory());
    }
}
