package kr.inuappcenterportal.inuportal.domain.reply.repository;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.reply.model.Reply;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReplyRepository extends JpaRepository<Reply,Long> {
    Optional<Reply> findByIdAndIsDeletedFalse(Long id);

    List<Reply> findAllByMemberAndIsDeletedFalse(Member member, Sort sort);
    Optional<Reply> findFirstByMemberAndPost(Member member, Post post);


    @Query("SELECT r FROM Reply r LEFT JOIN FETCH r.member m WHERE r.post = :post AND (r.isDeleted = false OR EXISTS ( SELECT 1 FROM Reply rr WHERE rr.reply = r AND rr.isDeleted = false )) ")
    List<Reply> findAllNonDeletedOrHavingChildren(@Param("post") Post post);

    @Query("SELECT r FROM Reply r LEFT JOIN FETCH r.member m WHERE r.post = :post AND r.isDeleted = false AND r.good>=5 ORDER BY r.good DESC, r.id DESC  LIMIT 3")
    List<Reply> findBestReplies(@Param("post") Post post);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reply r WHERE r.id =:id AND r.isDeleted = false")
    Optional<Reply> findByIdWithLock(Long id);
}
