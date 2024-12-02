package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import kr.inuappcenterportal.inuportal.domain.PostLike;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface LikePostRepository extends JpaRepository<PostLike,Long> {
    boolean existsByMemberAndPost(Member member, Post post);
    Optional<PostLike> findByMemberAndPost(Member member, Post post);
    @Query("SELECT l FROM PostLike l JOIN FETCH l.post p WHERE l.member=:member")
    List<PostLike> findAllByMember(Member member, Sort sort);

}
