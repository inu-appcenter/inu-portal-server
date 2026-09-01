package kr.inuappcenterportal.inuportal.domain.suggestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.suggestion.model.Suggestion;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "건의사항 리스트 응답 Dto")
@Getter
@NoArgsConstructor
public class SuggestionListResponse {

    @Schema(description = "총 페이지 수")
    private Integer pages;

    @Schema(description = "총 건의사항 수")
    private Long total;

    @Schema(description = "건의사항 리스트")
    private List<SuggestionResponse> suggestions;

    @Builder
    private SuggestionListResponse(int pages, long total, List<SuggestionResponse> suggestions) {
        this.pages = pages;
        this.total = total;
        this.suggestions = suggestions;
    }

    public static SuggestionListResponse of(Page<Suggestion> suggestionPage) {
        return SuggestionListResponse.builder()
                .pages(suggestionPage.getTotalPages())
                .total(suggestionPage.getTotalElements())
                .suggestions(suggestionPage.getContent().stream().map(SuggestionResponse::of).collect(Collectors.toList()))
                .build();
    }
}
