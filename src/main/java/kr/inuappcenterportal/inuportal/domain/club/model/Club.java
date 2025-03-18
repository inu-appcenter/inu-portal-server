package kr.inuappcenterportal.inuportal.domain.club.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "club")
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String name;
    @Column
    private String category;
    @Column(name = "image_url")
    private String imageUrl;
    @Column
    private String url;
    @Column(name = "home_url")
    private String homeUrl;
    @Column(name = "is_recruiting")
    private Boolean isRecruiting;
    @Column(name="image_count")
    private Long imageCount;
    @Column(name="recruit",length = 1000)
    private String recruit;
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Builder
    public Club(String name, String category, String imageUrl, String url, String homeUrl){
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.url = url;
        this.homeUrl = homeUrl;
        this.isRecruiting = false;
        this.imageCount = 0L;
        this.modifiedDate = LocalDateTime.now();
    }

    public void recruiting(String recruit, Long imageCount, Boolean isRecruiting){
        this.recruit = recruit;
        this.imageCount = imageCount;
        this.isRecruiting = isRecruiting;
        this.modifiedDate = LocalDateTime.now();
    }

}
