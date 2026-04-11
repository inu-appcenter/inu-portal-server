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

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "content_fetched_at")
    private LocalDateTime contentFetchedAt;

    private DepartmentNotice(Department department, String title, String createDate, Long view, String url) {
        this.department = department;
        this.title = title;
        this.createDate = createDate;
        this.view = view;
        this.url = url;
        this.contentStatus = DepartmentNoticeContentStatus.PENDING;
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

    public void updateContent(String contentHtml, String contentText, String contentHash, LocalDateTime contentFetchedAt) {
        this.contentHtml = contentHtml;
        this.contentText = contentText;
        this.contentHash = contentHash;
        this.contentFetchedAt = contentFetchedAt;
        this.contentStatus = DepartmentNoticeContentStatus.SUCCESS;
    }

    public void markContentFailed() {
        this.contentStatus = DepartmentNoticeContentStatus.FAILED;
    }

    public void markContentAccessDenied() {
        this.contentStatus = DepartmentNoticeContentStatus.ACCESS_DENIED;
    }

    public boolean hasContent() {
        return contentText != null && !contentText.isBlank();
    }

    public boolean isContentCrawlBlocked() {
        return contentStatus == DepartmentNoticeContentStatus.ACCESS_DENIED;
    }
}
