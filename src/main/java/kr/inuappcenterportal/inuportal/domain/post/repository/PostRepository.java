package kr.inuappcenterportal.inuportal.domain.post.repository;

import jakarta.persistence.LockModeType;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post,Long> {
    Optional<Post> findByIdAndIsDeletedFalse(Long id);
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.member m WHERE p.id = :id AND p.isDeleted = false")
    Optional<Post> findByIdAndIsDeletedFalseWithPostMember(Long id);
    Page<Post> findAllByCategoryAndIsDeletedFalse(String category,Pageable pageable);

    Page<Post> findAllByIsDeletedFalse(Pageable pageable);

    List<Post> findAllByIdLessThanAndIsDeletedFalse(Long id, Pageable pageable);
    List<Post> findByCategoryAndIdLessThanAndIsDeletedFalse(String category,Long id,Pageable pageable);
    long count();
    List<Post> findAllByMemberAndIsDeletedFalse(Member member, Sort sort);

    @Query(value = "SELECT * FROM post WHERE is_deleted = false AND MATCH(title, content) AGAINST(?1 IN NATURAL LANGUAGE MODE)", nativeQuery = true)
    Page<Post> searchByKeyword(String keyword,Pageable pageable);
    @Query(value = "SELECT * FROM post WHERE is_deleted = false AND MATCH(title, content) AGAINST(?1 IN BOOLEAN MODE) ORDER BY good DESC", nativeQuery = true)
    Page<Post> searchByKeywordOrderByLikes(String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM post WHERE is_deleted = false AND MATCH(title, content) AGAINST(?1 IN BOOLEAN MODE) ORDER BY scrap DESC", nativeQuery = true)
    Page<Post> searchByKeywordOrderByScraps(String keyword, Pageable pageable);
    @Query("SELECT p FROM Post p WHERE p.good >= 1 AND p.isDeleted = false AND (:category IS NULL OR p.category = :category) ORDER BY p.good DESC ,p.id DESC LIMIT 12")
    List<Post> findTop12(String category);

    @Query("SELECT p FROM Post p WHERE p.good>= 1 AND p.isDeleted = false ORDER BY RAND() LIMIT 9")
    List<Post> findRandomTop();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id =:id AND p.isDeleted = false")
    Optional<Post> findByIdWithLock(Long id);



}
