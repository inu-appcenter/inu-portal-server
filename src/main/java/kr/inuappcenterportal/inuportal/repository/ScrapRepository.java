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

    @Query("SELECT s FROM Scrap s JOIN FETCH s.post p WHERE s.member=:member ORDER BY p.id desc")
    List<Scrap> findAllByMember(Member member);
    @Query("SELECT s FROM Scrap s JOIN FETCH s.post p WHERE s.member=:member ORDER BY p.good desc")
    List<Scrap> findAllByMemberOrderByGood(Member member);
    @Query("SELECT s FROM Scrap s JOIN FETCH s.post p WHERE s.member=:member ORDER BY p.scrap desc")
    List<Scrap> findAllByMemberOrderByScrap(Member member);

    @Query("SELECT s FROM Scrap s JOIN FETCH s.post p WHERE s.member=:member and (s.post.title  LIKE CONCAT('%',:content,'%') or s.post.content LIKE CONCAT('%',:content,'%')) ORDER BY p.id desc ")
    List<Scrap> searchScrap(Member member, String content);
    @Query("SELECT s FROM Scrap s JOIN FETCH s.post p WHERE s.member=:member and (s.post.title  LIKE CONCAT('%',:content,'%') or s.post.content LIKE CONCAT('%',:content,'%')) ORDER BY p.good desc ")
    List<Scrap> searchScrapOrderByGood(Member member, String content);
    @Query("SELECT s FROM Scrap s JOIN FETCH s.post p WHERE s.member=:member and (s.post.title  LIKE CONCAT('%',:content,'%') or s.post.content LIKE CONCAT('%',:content,'%')) ORDER BY p.scrap desc ")
    List<Scrap> searchScrapOrderByScrap(Member member, String content);

}
