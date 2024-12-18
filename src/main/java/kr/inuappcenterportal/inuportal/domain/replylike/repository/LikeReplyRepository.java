package kr.inuappcenterportal.inuportal.domain.replylike.repository;

import io.lettuce.core.dynamic.annotation.Param;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.reply.model.Reply;
import kr.inuappcenterportal.inuportal.domain.replylike.model.ReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LikeReplyRepository extends JpaRepository<ReplyLike,Long> {
    boolean existsByMemberAndReply(Member member, Reply reply);
    Optional<ReplyLike> findByMemberAndReply(Member member, Reply reply);
    @Query("SELECT lr.reply.id FROM ReplyLike lr WHERE lr.member = :member AND lr.reply.id IN :replyIds")
    List<Long> findLikedReplyIdsByMember(@Param("member") Member member, @Param("replyIds")List<Long> replyIds);
}
