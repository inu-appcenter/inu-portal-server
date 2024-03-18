package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import kr.inuappcenterportal.inuportal.domain.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap,Long> {
    boolean existsByMemberAndPost(Member member, Post post);
    Optional<Scrap> findByMemberAndPost(Member member, Post post);

    List<Scrap> findAllByMember(Member member);

    @Query("SELECT s FROM Scrap s JOIN FETCH s.post WHERE s.member=:member and s.post.title  LIKE CONCAT('%',:content,'%') or s.post.content LIKE CONCAT('%',:content,'%')")
    List<Scrap> searchScrap(Member member, String content);
}
