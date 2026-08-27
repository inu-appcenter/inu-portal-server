package kr.inuappcenterportal.inuportal.domain.notice.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.notice.enums.NoticeContentStatus;
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
@Table(name = "notice")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String category;

    @Column(name = "sub_category")
    private String subCategory;

    @Column
    private String title;

    @Column
    private String writer;

    @Column(name = "create_date")
    private String createDate;

    @Column(length = 512, unique = true)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "content_status", length = 32)
    private NoticeContentStatus contentStatus;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "content_fetched_at")
    private LocalDateTime contentFetchedAt;

    @Column(name = "content_retry_count")
    private Integer contentRetryCount;

    @Column(name = "content_last_error", length = 500)
    private String contentLastError;

    @OneToOne(mappedBy = "notice", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private NoticeContent content;

    @Builder
    public Notice(String category, String subCategory, String title, String writer, String createDate, String url, String description) {
        this.category = category;
        this.subCategory = subCategory;
        this.title = title;
        this.writer = writer;
        this.createDate = createDate;
        this.url = url;
        this.description = description;
        this.contentStatus = NoticeContentStatus.PENDING;
        this.contentRetryCount = 0;
        this.content = NoticeContent.builder().notice(this).build();
    }

    public void update(String subCategory, String title, String writer, String description) {
        this.subCategory = subCategory;
        this.title = title;
        this.writer = writer;
        this.description = description;
    }

    public void updateContent(
            String contentHtml,
            String contentText,
            String contentHash,
            LocalDateTime contentFetchedAt,
            String inlineImageUrlsJson,
            String attachmentMetaJson
    ) {
        if (this.content == null) {
            this.content = NoticeContent.builder().notice(this).build();
        }
        this.content.updateContent(contentHtml, contentText, inlineImageUrlsJson, attachmentMetaJson);
        this.contentHash = contentHash;
        this.contentFetchedAt = contentFetchedAt;
        this.contentLastError = null;
    }

    public void updateEnrichmentTexts(String ocrText, String attachmentText) {
        if (this.content == null) {
            this.content = NoticeContent.builder().notice(this).build();
        }
        this.content.updateEnrichmentTexts(ocrText, attachmentText);
        this.contentLastError = null;
    }

    public void markContentEnrichPending() {
        this.contentStatus = NoticeContentStatus.ENRICH_PENDING;
        this.contentLastError = null;
    }

    public void markContentOcrPending() {
        this.contentStatus = NoticeContentStatus.OCR_PENDING;
        this.contentLastError = null;
    }

    public void markContentSuccess() {
        this.contentStatus = NoticeContentStatus.SUCCESS;
        this.contentLastError = null;
    }

    public void markNoTextContent() {
        this.contentStatus = NoticeContentStatus.NO_TEXT_CONTENT;
        this.contentLastError = null;
    }

    public void markContentFailed(String reason) {
        this.contentStatus = NoticeContentStatus.FAILED;
        this.contentRetryCount = (contentRetryCount == null ? 0 : contentRetryCount) + 1;
        this.contentLastError = reason;
    }

    public void markContentPending() {
        this.contentStatus = NoticeContentStatus.PENDING;
        this.contentLastError = null;
    }

    public void touchContentFetchedAt(LocalDateTime fetchedAt) {
        this.contentFetchedAt = fetchedAt;
        this.contentLastError = null;
    }

    public void markContentAccessDenied() {
        this.contentStatus = NoticeContentStatus.ACCESS_DENIED;
        this.contentLastError = null;
    }

    public boolean hasContent() {
        return content != null && content.getContentText() != null && !content.getContentText().isBlank();
    }

    public boolean hasMergedText() {
        return content != null && !content.getMergedText().isBlank();
    }

    public boolean hasContentCrawlMetadata() {
        return content != null && content.getInlineImageUrlsJson() != null && content.getAttachmentMetaJson() != null;
    }

    public String getMergedText() {
        return content == null ? "" : content.getMergedText();
    }

    public String getBestEffortText() {
        return content == null ? "" : content.getBestEffortText();
    }

    public String getContentHtml() {
        return content == null ? null : content.getContentHtml();
    }

    public String getContentText() {
        return content == null ? null : content.getContentText();
    }

    public String getAttachmentText() {
        return content == null ? null : content.getAttachmentText();
    }

    public String getOcrText() {
        return content == null ? null : content.getOcrText();
    }

    public String getAttachmentMetaJson() {
        return content == null ? null : content.getAttachmentMetaJson();
    }

    public String getInlineImageUrlsJson() {
        return content == null ? null : content.getInlineImageUrlsJson();
    }

    public boolean isContentCrawlBlocked() {
        return contentStatus == NoticeContentStatus.ACCESS_DENIED
                || contentStatus == NoticeContentStatus.NO_TEXT_CONTENT
                || contentStatus == NoticeContentStatus.OCR_PENDING
                || contentStatus == NoticeContentStatus.SUCCESS
                || contentStatus == NoticeContentStatus.ENRICH_PENDING;
    }
}

