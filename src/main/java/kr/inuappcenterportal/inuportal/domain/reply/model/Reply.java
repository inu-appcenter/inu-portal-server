package kr.inuappcenterportal.inuportal.domain.reply.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.replylike.model.ReplyLike;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
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
@Table(name = "reply")
public class Reply extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column
    private Boolean anonymous;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column
    private Long number;

    @Column
    private Long good;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="member_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member member;

    @OneToMany(mappedBy = "reply",fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    private List<ReplyLike> likeReplies;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Reply reply;

    @Builder
    public Reply(String content, Post post, boolean anonymous, Member member, Reply reply,long number){
        this.content = content;
        this.post = post;
        this.member = member;
        this.anonymous =anonymous;
        this.reply =reply;
        this.isDeleted = false;
        this.number = number;
        this.good = 0L;
    }

    public void update(String content, boolean anonymous){
        this.content =content;
        this.anonymous =anonymous;
    }

    public void onDelete(){
        this.isDeleted = true;
    }
    public void upLike(){
        this.good++;
    }

    public void downLike(){
        this.good--;
    }
}
