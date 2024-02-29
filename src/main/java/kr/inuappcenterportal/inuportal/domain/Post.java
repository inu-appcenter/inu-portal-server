package kr.inuappcenterportal.inuportal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String category;

    @Column
    private Boolean anonymous;

    @Column
    private Integer number;

    @Column
    private Long view;

    @Column
    private Integer imageCount;


    @OneToMany(mappedBy = "post",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private List<Scrap> scraps;

    @OneToMany(mappedBy = "post",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private List<PostLike> postLikes;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private List<Reply> replies;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private List<FolderPost> folderPosts;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member member;

    @Builder
    public Post(String title, String content, String category, Boolean anonymous, Member member, Integer imageCount){
        this.title = title;
        this.content = content;
        this.category = category;
        this.anonymous = anonymous;
        this.member = member;
        this.view = 0L;
        this.number = 0;
        this.imageCount = imageCount;
    }

    public void updateOnlyPost(String title, String content, String category, Boolean anonymous){
        this.title = title;
        this.content = content;
        this.category = category;
        this.anonymous = anonymous;
    }
    public void upNumber(){
        this.number++;
    }

    public void upViewCount(){this.view++;}

    public void updateImageCount(Integer imageCount){
        this.imageCount = imageCount;

    }


}
