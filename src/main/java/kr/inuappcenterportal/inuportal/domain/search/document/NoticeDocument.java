package kr.inuappcenterportal.inuportal.domain.search.document;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "intip-notices")
@Setting(settingPath = "elasticsearch/settings.json")
public class NoticeDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String content;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String writer;

    @Field(type = FieldType.Keyword, index = false)
    private String url;

    @Field(type = FieldType.Keyword)
    private String createDate;

    @Builder
    public NoticeDocument(Long id, String title, String content, String category, String writer, String url, String createDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.writer = writer;
        this.url = url;
        this.createDate = createDate;
    }
}
