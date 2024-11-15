package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import kr.inuappcenterportal.inuportal.domain.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReplyRepository extends JpaRepository<Reply,Long> {
    List<Reply> findAllByPostAndReplyIsNull(Post post);
    List<Reply> findAllByReply(Reply reply);
    List<Reply> findAllByPost(Post post);
    List<Reply> findAllByMember(Member member);
    List<Reply> findAllByMemberOrderByIdDesc(Member member);
    boolean existsByReply(Reply reply);
    boolean existsByMember(Member member);
    Optional<Reply> findByMember(Member member);

    Optional<Reply> findFirstByMember(Member member);

}
