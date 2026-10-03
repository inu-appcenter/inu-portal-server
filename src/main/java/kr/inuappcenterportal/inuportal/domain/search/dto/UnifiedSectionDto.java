package kr.inuappcenterportal.inuportal.domain.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "통합 검색 섹션 결과")
public class UnifiedSectionDto<T> {

    @Schema(description = "해당 섹션의 총 검색 일치 건수")
    private long totalCount;

    @Schema(description = "검색 결과 항목 리스트")
    @Builder.Default
    private List<T> items = Collections.emptyList();

    public static <T> UnifiedSectionDto<T> of(long totalCount, List<T> items) {
        return new UnifiedSectionDto<>(totalCount, items != null ? items : Collections.emptyList());
    }

    public static <T> UnifiedSectionDto<T> empty() {
        return new UnifiedSectionDto<>(0L, Collections.emptyList());
    }
}
