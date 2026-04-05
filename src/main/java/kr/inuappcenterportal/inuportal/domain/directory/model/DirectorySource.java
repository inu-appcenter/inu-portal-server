package kr.inuappcenterportal.inuportal.domain.directory.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectorySourceTemplateType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "directory_source",
        indexes = {
                @Index(name = "idx_directory_source_category", columnList = "category"),
                @Index(name = "idx_directory_source_template_type", columnList = "template_type"),
                @Index(name = "idx_directory_source_display_order", columnList = "display_order")
        }
)
public class DirectorySource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectoryCategory category;

    @Column(name = "parent_name", nullable = false)
    private String parentName;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "parent_url", length = 1024)
    private String parentUrl;

    @Column(name = "source_url", nullable = false, length = 1024)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false)
    private DirectorySourceTemplateType templateType;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Builder
    private DirectorySource(DirectoryCategory category, String parentName, String sectionName,
                            String sourceName, String parentUrl, String sourceUrl,
                            DirectorySourceTemplateType templateType, Integer displayOrder,
                            LocalDateTime lastSyncedAt) {
        this.category = category;
        this.parentName = parentName;
        this.sectionName = sectionName;
        this.sourceName = sourceName;
        this.parentUrl = parentUrl;
        this.sourceUrl = sourceUrl;
        this.templateType = templateType;
        this.displayOrder = displayOrder;
        this.lastSyncedAt = lastSyncedAt;
    }

    public static DirectorySource create(DirectoryCategory category, String parentName, String sectionName,
                                         String sourceName, String parentUrl, String sourceUrl,
                                         DirectorySourceTemplateType templateType, Integer displayOrder,
                                         LocalDateTime lastSyncedAt) {
        return DirectorySource.builder()
                .category(category)
                .parentName(parentName)
                .sectionName(sectionName)
                .sourceName(sourceName)
                .parentUrl(parentUrl)
                .sourceUrl(sourceUrl)
                .templateType(templateType)
                .displayOrder(displayOrder)
                .lastSyncedAt(lastSyncedAt)
                .build();
    }
}
