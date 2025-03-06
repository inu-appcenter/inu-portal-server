package kr.inuappcenterportal.inuportal.domain.post.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.folderPost.model.FolderPost;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.postLike.model.PostLike;
import kr.inuappcenterportal.inuportal.domain.reply.model.Reply;
import kr.inuappcenterportal.inuportal.domain.scrap.model.Scrap;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post", indexes = {
        @Index(name = "idx_is_deleted", columnList = "is_deleted")
})
public class Post{

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

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;


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
        this.isDeleted = false;
        this.createDate = LocalDate.now();
        this.modifiedDate = LocalDateTime.now();
    }

    public void updateOnlyPost(String title, String content, String category, boolean anonymous){
        this.title = title;
        this.content = content;
        this.category = category;
        this.anonymous = anonymous;
        this.modifiedDate = LocalDateTime.now();
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
    public void delete(){
        this.isDeleted = true;
    }


}
