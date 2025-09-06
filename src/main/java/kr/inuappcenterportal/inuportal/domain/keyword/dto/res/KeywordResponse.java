package kr.inuappcenterportal.inuportal.domain.keyword.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.keyword.domain.Keyword;

@Schema(description = "키워드 알림 생성 응답")
public record KeywordResponse(

        @Schema(description = "키워드 Id", example = "1")
        Long keywordId,

        @Schema(description = "회원 Id", example = "1")
        Long memberId,

        @Schema(description = "키워드", example = "수강신청")
        String keyword,

        @Schema(description = "알림 Type", example = "DEPARTMENT")
        FcmMessageType type,

        @Schema(description = "알림 카테고리")
        String department

) {
    public static KeywordResponse from(Keyword keyword) {
        return new KeywordResponse(keyword.getId(), keyword.getMemberId(),
                keyword.getKeyword(), keyword.getType(), keyword.getDepartment().getDepartmentName());
    }
}
