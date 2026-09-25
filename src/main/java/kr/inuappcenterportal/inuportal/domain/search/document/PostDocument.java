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
@Document(indexName = "intip-posts", createIndex = false)
@Setting(settingPath = "elasticsearch/settings.json")
public class PostDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String content;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String writer;

    @Field(type = FieldType.Integer)
    private Integer good;

    @Field(type = FieldType.Integer)
    private Integer scrap;

    @Field(type = FieldType.Keyword)
    private String createDate;

    @Builder
    public PostDocument(Long id, String title, String content, String category, String writer, Integer good, Integer scrap, String createDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.writer = writer;
        this.good = good != null ? good : 0;
        this.scrap = scrap != null ? scrap : 0;
        this.createDate = createDate;
    }
}
