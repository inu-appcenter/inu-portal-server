package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeReplyRepository extends JpaRepository<ReplyLike,Long> {
    boolean existsByMemberAndReply(Member member, Reply reply);
    Optional<ReplyLike> findByMemberAndReply(Member member, Reply reply);
}
