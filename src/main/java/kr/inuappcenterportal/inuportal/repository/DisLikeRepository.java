package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.DisLike;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisLikeRepository extends JpaRepository<DisLike,Long> {
    boolean existsByMemberAndPost(Member member, Post post);
    Optional<DisLike> findByMemberAndPost(Member member,Post post);
}
