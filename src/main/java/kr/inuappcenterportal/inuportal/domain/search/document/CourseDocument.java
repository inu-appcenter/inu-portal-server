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
@Document(indexName = "intip-courses", createIndex = false)
@Setting(settingPath = "elasticsearch/settings.json")
public class CourseDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String subjectNumber;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String title;

    @Field(type = FieldType.Text)
    private String englishTitle;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String professor;

    @Field(type = FieldType.Integer)
    private Integer credit;

    @Field(type = FieldType.Keyword)
    private String hyName;

    @Field(type = FieldType.Keyword)
    private String isuName;

    @Builder
    public CourseDocument(Long id, String subjectNumber, String title, String englishTitle, String professor, Integer credit, String hyName, String isuName) {
        this.id = id;
        this.subjectNumber = subjectNumber;
        this.title = title;
        this.englishTitle = englishTitle;
        this.professor = professor;
        this.credit = credit;
        this.hyName = hyName;
        this.isuName = isuName;
    }
}
