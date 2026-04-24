package kr.inuappcenterportal.inuportal.domain.notice.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.enums.DepartmentNoticeContentStatus;
import kr.inuappcenterportal.inuportal.domain.notice.enums.DepartmentNoticeScheduleExtractStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "department_notice", indexes = {
        @Index(name = "idx_dept_url", columnList = "department, url", unique = true),
        @Index(name = "idx_dept_date_id", columnList = "department, create_date DESC, id DESC"),
        @Index(name = "idx_dept_title_date", columnList = "department, title, create_date"),
        @Index(name = "idx_content_extract_status", columnList = "content_status, schedule_extract_status")
})
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
    private LocalDate createDate;

    @Column(nullable = false)
    private Long view;

    @Column(nullable = false, length = 512)
    private String url;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "content_status", length = 32)
    private DepartmentNoticeContentStatus contentStatus;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "content_fetched_at")
    private LocalDateTime contentFetchedAt;

    @Column(name = "content_retry_count")
    private Integer contentRetryCount;

    @Column(name = "content_last_error", length = 500)
    private String contentLastError;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "schedule_extract_status", length = 32)
    private DepartmentNoticeScheduleExtractStatus scheduleExtractStatus;

    @Column(name = "schedule_extracted_at")
    private LocalDateTime scheduleExtractedAt;

    @Column(name = "schedule_extract_count")
    private Integer scheduleExtractCount;

    @Column(name = "schedule_extract_retry_count")
    private Integer scheduleExtractRetryCount;

    @Column(name = "schedule_extract_last_error", length = 500)
    private String scheduleExtractLastError;

    @OneToOne(mappedBy = "notice", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private DepartmentNoticeContent content;

    private DepartmentNotice(Department department, String title, LocalDate createDate, Long view, String url) {
        this.department = department;
        this.title = title;
        this.createDate = createDate;
        this.view = view;
        this.url = url;
        this.contentStatus = DepartmentNoticeContentStatus.PENDING;
        this.contentRetryCount = 0;
        this.scheduleExtractStatus = DepartmentNoticeScheduleExtractStatus.PENDING;
        this.scheduleExtractRetryCount = 0;
        this.content = DepartmentNoticeContent.builder().notice(this).build();
    }

    public static DepartmentNotice create(Department department, String title, LocalDate createDate, Long view, String url) {
        return new DepartmentNotice(department, title, createDate, view, url);
    }

    public void updateListing(String title, LocalDate createDate, Long view, String url) {
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
        this.content.updateContent(contentHtml, contentText, inlineImageUrlsJson, attachmentMetaJson);
        this.contentHash = contentHash;
        this.contentFetchedAt = contentFetchedAt;
        this.contentLastError = null;
        resetScheduleExtraction();
    }

    public void updateEnrichmentTexts(String ocrText, String attachmentText) {
        this.content.updateEnrichmentTexts(ocrText, attachmentText);
        this.contentLastError = null;
        resetScheduleExtraction();
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

    public void markScheduleExtractProcessing() {
        this.scheduleExtractStatus = DepartmentNoticeScheduleExtractStatus.PROCESSING;
        this.scheduleExtractLastError = null;
    }

    public void markScheduleExtractSuccess(int count, String responseJson) {
        this.scheduleExtractStatus = DepartmentNoticeScheduleExtractStatus.SUCCESS;
        this.scheduleExtractedAt = LocalDateTime.now();
        this.scheduleExtractCount = count;
        this.content.updateScheduleExtractResponse(responseJson);
        this.scheduleExtractLastError = null;
    }

    public void markScheduleNoSchedule(String responseJson) {
        this.scheduleExtractStatus = DepartmentNoticeScheduleExtractStatus.NO_SCHEDULE;
        this.scheduleExtractedAt = LocalDateTime.now();
        this.scheduleExtractCount = 0;
        this.content.updateScheduleExtractResponse(responseJson);
        this.scheduleExtractLastError = null;
    }

    public void markScheduleExtractFailed(String reason) {
        this.scheduleExtractStatus = DepartmentNoticeScheduleExtractStatus.FAILED;
        this.scheduleExtractRetryCount = (scheduleExtractRetryCount == null ? 0 : scheduleExtractRetryCount) + 1;
        this.scheduleExtractLastError = reason;
    }

    public void resetScheduleExtraction() {
        this.scheduleExtractStatus = DepartmentNoticeScheduleExtractStatus.PENDING;
        this.scheduleExtractedAt = null;
        this.scheduleExtractCount = null;
        this.scheduleExtractLastError = null;
        this.content.resetScheduleExtraction();
        this.scheduleExtractRetryCount = 0;
    }

    public boolean hasContent() {
        return content.getContentText() != null && !content.getContentText().isBlank();
    }

    public boolean hasMergedText() {
        return !content.getMergedText().isBlank();
    }

    public boolean hasContentCrawlMetadata() {
        return content.getInlineImageUrlsJson() != null && content.getAttachmentMetaJson() != null;
    }

    public String getMergedText() {
        return content.getMergedText();
    }

    public String getBestEffortText() {
        return content.getBestEffortText();
    }

    public String getContentText() {
        return content.getContentText();
    }

    public String getAttachmentText() {
        return content.getAttachmentText();
    }

    public String getOcrText() {
        return content.getOcrText();
    }

    public String getAttachmentMetaJson() {
        return content.getAttachmentMetaJson();
    }

    public String getInlineImageUrlsJson() {
        return content.getInlineImageUrlsJson();
    }

    public boolean isContentCrawlBlocked() {
        return contentStatus == DepartmentNoticeContentStatus.ACCESS_DENIED
                || contentStatus == DepartmentNoticeContentStatus.NO_TEXT_CONTENT
                || contentStatus == DepartmentNoticeContentStatus.OCR_PENDING
                || contentStatus == DepartmentNoticeContentStatus.SUCCESS
                || contentStatus == DepartmentNoticeContentStatus.ENRICH_PENDING;
    }
}
