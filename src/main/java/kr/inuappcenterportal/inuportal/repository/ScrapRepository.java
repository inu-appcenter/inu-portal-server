package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import kr.inuappcenterportal.inuportal.domain.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap,Long> {
    boolean existsByMemberAndPost(Member member, Post post);
    Optional<Scrap> findByMemberAndPost(Member member, Post post);
}
