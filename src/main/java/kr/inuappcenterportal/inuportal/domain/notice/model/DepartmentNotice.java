package kr.inuappcenterportal.inuportal.domain.notice.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.enums.DepartmentNoticeContentStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "department_notice")
public class DepartmentNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Department department;

    @Column(nullable = false)
    private String title;

    @Column(name="create_date", nullable = false)
    private String createDate;

    @Column(nullable = false)
    private Long view;

    @Column(nullable = false, length = 512)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_status", length = 32)
    private DepartmentNoticeContentStatus contentStatus;

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

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "content_fetched_at")
    private LocalDateTime contentFetchedAt;

    @Column(name = "content_retry_count")
    private Integer contentRetryCount;

    @Column(name = "content_last_error", length = 500)
    private String contentLastError;

    private DepartmentNotice(Department department, String title, String createDate, Long view, String url) {
        this.department = department;
        this.title = title;
        this.createDate = createDate;
        this.view = view;
        this.url = url;
        this.contentStatus = DepartmentNoticeContentStatus.PENDING;
        this.contentRetryCount = 0;
    }

    public static DepartmentNotice create(Department department, String title, String createDate, Long view, String url) {
        return new DepartmentNotice(department, title, createDate, view, url);
    }

    public void updateListing(String title, String createDate, Long view, String url) {
        this.title = title;
        this.createDate = createDate;
        this.view = view;
        this.url = url;
    }

    public void updateView(Long view) {
        this.view = view;
    }

    public void updateContent(
            String contentHtml,
            String contentText,
            String contentHash,
            LocalDateTime contentFetchedAt,
            String inlineImageUrlsJson,
            String attachmentMetaJson
    ) {
        this.contentHtml = contentHtml;
        this.contentText = contentText;
        this.contentHash = contentHash;
        this.contentFetchedAt = contentFetchedAt;
        this.inlineImageUrlsJson = inlineImageUrlsJson;
        this.attachmentMetaJson = attachmentMetaJson;
        this.contentLastError = null;
    }

    public void updateEnrichmentTexts(String ocrText, String attachmentText, String mergedText) {
        this.ocrText = ocrText;
        this.attachmentText = attachmentText;
        this.mergedText = mergedText;
        this.contentLastError = null;
    }

    public void markContentEnrichPending() {
        this.contentStatus = DepartmentNoticeContentStatus.ENRICH_PENDING;
        this.contentLastError = null;
    }

    public void markContentOcrPending() {
        this.contentStatus = DepartmentNoticeContentStatus.OCR_PENDING;
        this.contentLastError = null;
    }

    public void markContentSuccess() {
        this.contentStatus = DepartmentNoticeContentStatus.SUCCESS;
        this.contentLastError = null;
    }

    public void markNoTextContent() {
        this.contentStatus = DepartmentNoticeContentStatus.NO_TEXT_CONTENT;
        this.contentLastError = null;
    }

    public void markContentFailed(String reason) {
        this.contentStatus = DepartmentNoticeContentStatus.FAILED;
        this.contentRetryCount = (contentRetryCount == null ? 0 : contentRetryCount) + 1;
        this.contentLastError = reason;
    }

    public void markContentAccessDenied() {
        this.contentStatus = DepartmentNoticeContentStatus.ACCESS_DENIED;
        this.contentLastError = null;
    }

    public boolean hasContent() {
        return contentText != null && !contentText.isBlank();
    }

    public boolean hasMergedText() {
        return mergedText != null && !mergedText.isBlank();
    }

    public boolean hasContentCrawlMetadata() {
        return inlineImageUrlsJson != null && attachmentMetaJson != null;
    }

    public String getBestEffortText() {
        if (hasMergedText()) {
            return mergedText;
        }
        if (hasContent()) {
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

    public boolean isContentCrawlBlocked() {
        return contentStatus == DepartmentNoticeContentStatus.ACCESS_DENIED
                || contentStatus == DepartmentNoticeContentStatus.NO_TEXT_CONTENT
                || contentStatus == DepartmentNoticeContentStatus.OCR_PENDING
                || contentStatus == DepartmentNoticeContentStatus.SUCCESS
                || contentStatus == DepartmentNoticeContentStatus.ENRICH_PENDING;
    }
}
