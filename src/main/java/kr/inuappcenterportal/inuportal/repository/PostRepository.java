package kr.inuappcenterportal.inuportal.repository;

import jakarta.persistence.LockModeType;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post,Long> {
    Page<Post> findAllByCategory (String category,Pageable pageable);

    Page<Post> findAllBy(Pageable pageable);

    List<Post> findAllByIdLessThan(Long id, Pageable pageable);
    List<Post> findByCategoryAndIdLessThan(String category,Long id,Pageable pageable);
    long count();
    List<Post> findAllByMember(Member member, Sort sort);

    @Query(value = "SELECT * FROM post WHERE MATCH(title, content) AGAINST(?1 IN NATURAL LANGUAGE MODE)", nativeQuery = true)
    Page<Post> searchByKeyword(String keyword,Pageable pageable);
    @Query(value = "SELECT * FROM post WHERE MATCH(title, content) AGAINST(?1 IN BOOLEAN MODE) ORDER BY good DESC", nativeQuery = true)
    Page<Post> searchByKeywordOrderByLikes(String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM post WHERE MATCH(title, content) AGAINST(?1 IN BOOLEAN MODE) ORDER BY scrap DESC", nativeQuery = true)
    Page<Post> searchByKeywordOrderByScraps(String keyword, Pageable pageable);
    @Query("SELECT p FROM Post p WHERE p.good >= 1 AND (:category IS NULL OR p.category = :category) ORDER BY p.good DESC ,p.id DESC LIMIT 12")
    List<Post> findTop12(String category);

    @Query("SELECT p FROM Post p WHERE p.good>= 1 ORDER BY RAND() LIMIT 9")
    List<Post> findRandomTop();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id =:id")
    Optional<Post> findByIdWithLock(Long id);



}
