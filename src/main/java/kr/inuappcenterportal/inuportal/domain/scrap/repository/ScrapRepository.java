package kr.inuappcenterportal.inuportal.domain.scrap.repository;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.scrap.model.Scrap;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap,Long> {
    boolean existsByMemberAndPost(Member member, Post post);
    Optional<Scrap> findByMemberAndPost(Member member, Post post);


    @Query("SELECT s FROM Scrap s JOIN FETCH s.post p WHERE s.member=:member")
    List<Scrap> findAllByMember(Member member, Sort sort);

    @Query("SELECT s FROM Scrap s JOIN FETCH s.post p WHERE s.member=:member and (s.post.title  LIKE CONCAT('%',:content,'%') or s.post.content LIKE CONCAT('%',:content,'%'))")
    List<Scrap> searchScrap(Member member, String content,Sort sort);



}
