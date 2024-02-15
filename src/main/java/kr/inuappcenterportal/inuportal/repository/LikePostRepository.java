package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.PostLike;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface LikePostRepository extends JpaRepository<PostLike,Long> {
    boolean existsByMemberAndPost(Member member, Post post);
    Optional<PostLike> findByMemberAndPost(Member member, Post post);
    List<PostLike> findAllByMember(Member member);
}
