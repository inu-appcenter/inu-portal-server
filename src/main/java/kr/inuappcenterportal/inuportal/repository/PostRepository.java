package kr.inuappcenterportal.inuportal.repository;

import jakarta.persistence.LockModeType;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post,Long> {
    Page<Post> findAllByCategoryOrderByIdDesc (String category,Pageable pageable);
    Page<Post> findAllByCategoryOrderByGoodDescIdDesc (String category,Pageable pageable);
    Page<Post> findAllByCategoryOrderByScrapDescIdDesc (String category,Pageable pageable);
    Long countAllByCategory (String category);
    Page<Post> findAllByOrderByIdDesc(Pageable pageable);
    Page<Post> findAllByOrderByGoodDescIdDesc(Pageable pageable);
    Page<Post> findAllByOrderByScrapDescIdDesc(Pageable pageable);
    long count();
    Page<Post> findAllByMemberOrderByIdDesc(Member member,Pageable pageable);
    Page<Post> findAllByMemberOrderByGoodDescIdDesc(Member member,Pageable pageable);
    Page<Post> findAllByMemberOrderByScrapDescIdDesc(Member member,Pageable pageable);
    Page<Post> findAllByTitleContainsOrContentContainsOrderByIdDesc(String title, String content, Pageable pageable);
    Page<Post> findAllByTitleContainsOrContentContainsOrderByGoodDescIdDesc(String title,String content,Pageable pageable);
    Page<Post> findAllByTitleContainsOrContentContainsOrderByScrapDescIdDesc(String title,String content,Pageable pageable);
    Long countAllByTitleContainsOrContentContains(String title,String content);
    @Query("SELECT p FROM Post p WHERE p.good >= 1 AND (:category IS NULL OR p.category = :category) ORDER BY p.good DESC ,p.id DESC LIMIT 12")
    List<Post> findTop12(String category);

    @Query("SELECT p FROM Post p WHERE p.good>= 1 ORDER BY RAND() LIMIT 9")
    List<Post> findRandomTop();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id =:id")
    Optional<Post> findByIdWithLock(Long id);



}
