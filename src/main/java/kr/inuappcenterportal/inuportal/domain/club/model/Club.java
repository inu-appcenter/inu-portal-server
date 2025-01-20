package kr.inuappcenterportal.inuportal.domain.club.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Builder
    public Club(String name, String category, String imageUrl, String url, String homeUrl){
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.url = url;
        this.homeUrl = homeUrl;
    }


}
