package kr.inuappcenterportal.inuportal.domain.search;

import kr.inuappcenterportal.inuportal.domain.search.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedSearchDtoTest {

    @Test
    @DisplayName("통합 검색 응답 DTO 생성 및 섹션 데이터 조립 테스트")
    void testUnifiedSearchResponseDto() {
        NoticeSearchItemDto noticeItem = NoticeSearchItemDto.builder()
                .id(1L)
                .title("<mark>장학금</mark> 신청 안내")
                .snippet("교내 성적우수 <mark>장학금</mark> 지급...")
                .writer("장학지원과")
                .category("학사")
                .build();

        UnifiedSectionDto<NoticeSearchItemDto> noticeSection = UnifiedSectionDto.of(1L, List.of(noticeItem));

        UnifiedSearchResponseDto response = UnifiedSearchResponseDto.builder()
                .query("장학금")
                .tab(SearchTab.ALL)
                .totalCount(1L)
                .notices(noticeSection)
                .build();

        assertThat(response.getQuery()).isEqualTo("장학금");
        assertThat(response.getTab()).isEqualTo(SearchTab.ALL);
        assertThat(response.getTotalCount()).isEqualTo(1L);
        assertThat(response.getNotices().getItems()).hasSize(1);
        assertThat(response.getNotices().getItems().get(0).getTitle()).contains("<mark>장학금</mark>");
    }

    @Test
    @DisplayName("빈 섹션 생성 테스트")
    void testEmptySection() {
        UnifiedSectionDto<NoticeSearchItemDto> emptySection = UnifiedSectionDto.empty();
        assertThat(emptySection.getTotalCount()).isEqualTo(0L);
        assertThat(emptySection.getItems()).isEmpty();
    }
}
