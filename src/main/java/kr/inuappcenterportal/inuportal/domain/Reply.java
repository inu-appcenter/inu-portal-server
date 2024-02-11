package kr.inuappcenterportal.inuportal.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Reply extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column
    private Boolean anonymous;

    @Column
    private Boolean delete;

    @Column
    private Integer number;

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
    public Reply(String content, Post post, Boolean anonymous, Member member, Reply reply,Integer number){
        this.content = content;
        this.post = post;
        this.member = member;
        this.anonymous =anonymous;
        this.reply =reply;
        this.delete = false;
        this.number = number;
    }

    public void update(String content, Boolean anonymous){
        this.content =content;
        this.anonymous =anonymous;
    }

    public void onDelete(String content, Member member){
        this.content =content;
        this.member = member;
        this.delete = true;
    }
}
