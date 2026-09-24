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
@Document(indexName = "intip-schedules")
@Setting(settingPath = "elasticsearch/settings.json")
public class ScheduleDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer", searchAnalyzer = "korean_search_analyzer")
    private String content;

    @Field(type = FieldType.Keyword)
    private String startDate;

    @Field(type = FieldType.Keyword)
    private String endDate;

    @Field(type = FieldType.Keyword)
    private String department;

    @Field(type = FieldType.Boolean)
    private Boolean aiGenerated;

    @Builder
    public ScheduleDocument(Long id, String content, String startDate, String endDate, String department, Boolean aiGenerated) {
        this.id = id;
        this.content = content;
        this.startDate = startDate;
        this.endDate = endDate;
        this.department = department;
        this.aiGenerated = aiGenerated != null ? aiGenerated : false;
    }
}
