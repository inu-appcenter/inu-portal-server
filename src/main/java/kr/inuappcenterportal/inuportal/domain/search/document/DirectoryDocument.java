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
@Document(indexName = "intip-directory", createIndex = false)
@Setting(settingPath = "elasticsearch/settings.json")
public class DirectoryDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String name;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String affiliation;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String detailAffiliation;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String position;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String duties;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Keyword)
    private String phoneNumber;

    @Builder
    public DirectoryDocument(Long id, String name, String affiliation, String detailAffiliation, String position, String duties, String email, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.affiliation = affiliation;
        this.detailAffiliation = detailAffiliation;
        this.position = position;
        this.duties = duties;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }
}
