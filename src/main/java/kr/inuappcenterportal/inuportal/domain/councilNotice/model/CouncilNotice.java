package kr.inuappcenterportal.inuportal.domain.councilNotice.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "council_notice")
public class CouncilNotice{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String content;

    @Column
    private Long view;

    @Column(name="image_count")
    private Long imageCount;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Builder
    public CouncilNotice (String title, String content){
        this.title = title;
        this.content = content;
        this.imageCount = 0L;
        this.view = 0L;
        this.createDate = LocalDate.now();
        this.modifiedDate = LocalDateTime.now();
    }

    public void upViewCount(){this.view++;}

    public void updateImageCount(long imageCount){
        this.imageCount = imageCount;
    }

    public void updateCouncilNotice(String title, String content){
        this.title = title;
        this.content = content;
        this.modifiedDate = LocalDateTime.now();
    }

}
