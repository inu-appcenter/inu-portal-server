package kr.inuappcenterportal.inuportal.domain.petition.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "petition")
public class Petition{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false,length = 2000)
    private String content;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column
    private Long good;

    @Column
    private Long view;

    @Column(name="image_count")
    private Long imageCount;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    public Petition (String title, String content, Boolean isPrivate, Member member){
        this.title = title;
        this.content = content;
        this.isPrivate = isPrivate;
        this.good = 0L;
        this.view = 0L;
        this.imageCount = 0L;
        this.isDeleted = false;
        this.member = member;
        this.createDate = LocalDate.now();
        this.modifiedDate = LocalDateTime.now();
    }

    public void updatePetition(String title, String content, Boolean isPrivate){
        this.title = title;
        this.content = content;
        this.isPrivate = isPrivate;
        this.modifiedDate = LocalDateTime.now();
    }

    public void deletePetition(){
        this.isDeleted = true;
    }

    public void updateImageCount(long imageCount){
        this.imageCount = imageCount;
    }

    public void upViewCount(){
        this.view++;
    }
    public void upLike(){
        this.good++;
    }

    public void downLike(){
        this.good--;
    }
}
