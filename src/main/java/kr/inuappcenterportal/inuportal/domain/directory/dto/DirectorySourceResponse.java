package kr.inuappcenterportal.inuportal.domain.directory.dto;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectorySourceTemplateType;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DirectorySourceResponse {

    private final Long id;
    private final DirectoryCategory category;
    private final String categoryName;
    private final String parentName;
    private final String sectionName;
    private final String sourceName;
    private final String parentUrl;
    private final String sourceUrl;
    private final DirectorySourceTemplateType templateType;
    private final String templateTypeName;
    private final LocalDateTime lastSyncedAt;

    @Builder
    private DirectorySourceResponse(Long id, DirectoryCategory category, String categoryName,
                                    String parentName, String sectionName, String sourceName,
                                    String parentUrl, String sourceUrl,
                                    DirectorySourceTemplateType templateType, String templateTypeName,
                                    LocalDateTime lastSyncedAt) {
        this.id = id;
        this.category = category;
        this.categoryName = categoryName;
        this.parentName = parentName;
        this.sectionName = sectionName;
        this.sourceName = sourceName;
        this.parentUrl = parentUrl;
        this.sourceUrl = sourceUrl;
        this.templateType = templateType;
        this.templateTypeName = templateTypeName;
        this.lastSyncedAt = lastSyncedAt;
    }

    public static DirectorySourceResponse of(DirectorySource source) {
        return DirectorySourceResponse.builder()
                .id(source.getId())
                .category(source.getCategory())
                .categoryName(source.getCategory().getLabel())
                .parentName(source.getParentName())
                .sectionName(source.getSectionName())
                .sourceName(source.getSourceName())
                .parentUrl(source.getParentUrl())
                .sourceUrl(source.getSourceUrl())
                .templateType(source.getTemplateType())
                .templateTypeName(source.getTemplateType().getLabel())
                .lastSyncedAt(source.getLastSyncedAt())
                .build();
    }
}
