package kr.inuappcenterportal.inuportal.domain.postLike.repository;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.postLike.model.PostLike;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface LikePostRepository extends JpaRepository<PostLike,Long> {
    boolean existsByMemberAndPost(Member member, Post post);
    Optional<PostLike> findByMemberAndPost(Member member, Post post);
    @Query("SELECT l FROM PostLike l JOIN FETCH l.post p WHERE l.member=:member AND p.isDeleted = false")
    List<PostLike> findAllByMember(Member member, Sort sort);

}
