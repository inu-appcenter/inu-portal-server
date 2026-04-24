package kr.inuappcenterportal.inuportal.domain.notice.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "department_notice_content")
public class DepartmentNoticeContent {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private DepartmentNotice notice;

    @Lob
    @Column(name = "content_html", columnDefinition = "LONGTEXT")
    private String contentHtml;

    @Lob
    @Column(name = "content_text", columnDefinition = "LONGTEXT")
    private String contentText;

    @Lob
    @Column(name = "ocr_text", columnDefinition = "LONGTEXT")
    private String ocrText;

    @Lob
    @Column(name = "attachment_text", columnDefinition = "LONGTEXT")
    private String attachmentText;

    @Lob
    @Column(name = "merged_text", columnDefinition = "LONGTEXT")
    private String mergedText;

    @Lob
    @Column(name = "inline_image_urls_json", columnDefinition = "LONGTEXT")
    private String inlineImageUrlsJson;

    @Lob
    @Column(name = "attachment_meta_json", columnDefinition = "LONGTEXT")
    private String attachmentMetaJson;

    @Lob
    @Column(name = "schedule_extract_response_json", columnDefinition = "LONGTEXT")
    private String scheduleExtractResponseJson;

    @Builder
    public DepartmentNoticeContent(DepartmentNotice notice, String contentHtml, String contentText, String ocrText,
                                 String attachmentText, String mergedText, String inlineImageUrlsJson,
                                 String attachmentMetaJson, String scheduleExtractResponseJson) {
        this.notice = notice;
        this.contentHtml = contentHtml;
        this.contentText = contentText;
        this.ocrText = ocrText;
        this.attachmentText = attachmentText;
        this.mergedText = mergedText;
        this.inlineImageUrlsJson = inlineImageUrlsJson;
        this.attachmentMetaJson = attachmentMetaJson;
        this.scheduleExtractResponseJson = scheduleExtractResponseJson;
    }

    public void updateContent(
            String contentHtml,
            String contentText,
            String inlineImageUrlsJson,
            String attachmentMetaJson
    ) {
        this.contentHtml = contentHtml;
        this.contentText = contentText;
        this.inlineImageUrlsJson = inlineImageUrlsJson;
        this.attachmentMetaJson = attachmentMetaJson;
    }

    public void updateEnrichmentTexts(String ocrText, String attachmentText, String mergedText) {
        this.ocrText = ocrText;
        this.attachmentText = attachmentText;
        this.mergedText = mergedText;
    }

    public void updateScheduleExtractResponse(String responseJson) {
        this.scheduleExtractResponseJson = responseJson;
    }

    public void resetScheduleExtraction() {
        this.scheduleExtractResponseJson = null;
    }

    public String getBestEffortText() {
        if (mergedText != null && !mergedText.isBlank()) {
            return mergedText;
        }
        if (contentText != null && !contentText.isBlank()) {
            return contentText;
        }
        if (attachmentText != null && !attachmentText.isBlank()) {
            return attachmentText;
        }
        if (ocrText != null && !ocrText.isBlank()) {
            return ocrText;
        }
        return "";
    }
}
