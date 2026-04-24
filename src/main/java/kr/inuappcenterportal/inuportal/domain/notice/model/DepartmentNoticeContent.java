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
    @Column(name = "inline_image_urls_json", columnDefinition = "LONGTEXT")
    private String inlineImageUrlsJson;

    @Lob
    @Column(name = "attachment_meta_json", columnDefinition = "LONGTEXT")
    private String attachmentMetaJson;

    @Builder
    public DepartmentNoticeContent(DepartmentNotice notice, String contentHtml, String contentText, String ocrText,
                                 String attachmentText, String inlineImageUrlsJson,
                                 String attachmentMetaJson) {
        this.notice = notice;
        this.contentHtml = contentHtml;
        this.contentText = contentText;
        this.ocrText = ocrText;
        this.attachmentText = attachmentText;
        this.inlineImageUrlsJson = inlineImageUrlsJson;
        this.attachmentMetaJson = attachmentMetaJson;
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

    public void updateEnrichmentTexts(String ocrText, String attachmentText) {
        this.ocrText = ocrText;
        this.attachmentText = attachmentText;
    }

    public void resetScheduleExtraction() {
        // No-op or keep empty if needed, as response JSON is removed
    }


    public String getMergedText() {
        java.util.List<String> texts = new java.util.ArrayList<>();
        if (contentText != null && !contentText.isBlank()) texts.add(contentText.trim());
        if (attachmentText != null && !attachmentText.isBlank()) texts.add(attachmentText.trim());
        if (ocrText != null && !ocrText.isBlank()) texts.add(ocrText.trim());
        return String.join("\n\n", texts);
    }

    public String getBestEffortText() {
        String merged = getMergedText();
        if (!merged.isBlank()) {
            return merged;
        }
        return "";
    }
}
