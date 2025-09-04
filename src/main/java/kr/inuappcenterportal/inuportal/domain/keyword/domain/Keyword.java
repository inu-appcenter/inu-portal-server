package kr.inuappcenterportal.inuportal.domain.keyword.domain;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.keyword.enums.KeywordCategory;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String keyword;

    @Column(nullable = false)
    private KeywordCategory keywordCategory;

    @Builder
    private Keyword(Long memberId, String keyword, KeywordCategory keywordCategory) {
        this.memberId = memberId;
        this.keyword = keyword;
        this.keywordCategory = keywordCategory;
    }
}
