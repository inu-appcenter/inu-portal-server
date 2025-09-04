package kr.inuappcenterportal.inuportal.domain.keyword.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import kr.inuappcenterportal.inuportal.domain.keyword.enums.KeywordCategory;

@Schema(description = "키워드 알림 생성 요청")
public record KeywordRequest(

        @Schema(description = "키워드", example = "수강신청")
        @NotBlank(message = "키워드가 비어있습니다.")
        String keyword,

        @Schema(description = "알림 카테고리")
        @NotBlank(message = "알림 카테고리가 비어있습니다.")
        KeywordCategory keywordCategory

) {
}
