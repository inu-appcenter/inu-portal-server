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
@Table(name = "post")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false,length = 2000)
    private String content;

    @Column(nullable = false)
    private String category;

    @Column
    private Boolean anonymous;

    @Column
    private Long number;

    @Column
    private Long good;

    @Column
    private Long scrap;

    @Column
    private Long view;

    @Column(name="image_count")
    private Long imageCount;

    @Column(name = "reply_count")
    private Long replyCount;


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
    public Post(String title, String content, String category, boolean anonymous, Member member, long imageCount){
        this.title = title;
        this.content = content;
        this.category = category;
        this.anonymous = anonymous;
        this.member = member;
        this.view = 0L;
        this.number = 0L;
        this.imageCount = imageCount;
        this.good = 0L;
        this.scrap = 0L;
        this.replyCount = 0L;
    }

    public void updateOnlyPost(String title, String content, String category, boolean anonymous){
        this.title = title;
        this.content = content;
        this.category = category;
        this.anonymous = anonymous;
    }
    public void upNumber(){
        this.number++;
    }

    public void upViewCount(){this.view++;}

    public void updateImageCount(long imageCount){
        this.imageCount = imageCount;
    }

    public void upLike(){
        this.good++;
    }

    public void downLike(){
        this.good--;
    }

    public void upScrap(){
        this.scrap++;
    }

    public void downScrap(){
        this.scrap--;
    }
    public void upReplyCount(){this.replyCount++;}
    public void downReplyCount(){this.replyCount--;}
    public void setReplyCount(long count){
        this.replyCount = count;
    }



}
