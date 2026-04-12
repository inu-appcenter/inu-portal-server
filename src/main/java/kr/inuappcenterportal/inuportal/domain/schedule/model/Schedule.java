package kr.inuappcenterportal.inuportal.domain.schedule.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "schedule")
public class Schedule {
    @Id
    private Long id;

    @Column(name="start_date")
    private LocalDate startDate;

    @Column(name="end_date")
    private LocalDate endDate;

    @Column
    private String content;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column
    private Department department;

    @Column(name = "source_notice_id")
    private Long sourceNoticeId;

    @Column(name = "source_url", length = 512)
    private String sourceUrl;

    @Column(name = "ai_generated")
    private boolean aiGenerated;

    @Builder
    public Schedule (Long id, LocalDate startDate, LocalDate endDate, String content, String description, Department department, Long sourceNoticeId, String sourceUrl, boolean aiGenerated){
        this.id = id;
        this.startDate =startDate;
        this.endDate = endDate;
        this.content = content;
        this.description = description;
        this.department = department;
        this.sourceNoticeId = sourceNoticeId;
        this.sourceUrl = sourceUrl;
        this.aiGenerated = aiGenerated;
    }




}
