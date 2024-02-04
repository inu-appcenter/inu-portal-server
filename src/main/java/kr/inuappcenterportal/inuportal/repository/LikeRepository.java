package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Good;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface LikeRepository extends JpaRepository<Good,Long> {
    boolean existsByMemberAndPost(Member member, Post post);
    Optional<Good> findByMemberAndPost(Member member,Post post);
}
