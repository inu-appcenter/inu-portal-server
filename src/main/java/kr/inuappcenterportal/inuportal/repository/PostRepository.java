package kr.inuappcenterportal.inuportal.repository;

import jakarta.persistence.LockModeType;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post,Long> {
    List<Post> findAllByCategoryOrderByIdDesc (String category,Pageable pageable);
    List<Post> findAllByCategoryOrderByGoodDescIdDesc (String category,Pageable pageable);
    List<Post> findAllByCategoryOrderByScrapDescIdDesc (String category,Pageable pageable);
    Long countAllByCategory (String category);
    List<Post> findAllByOrderByIdDesc(Pageable pageable);
    List<Post> findAllByOrderByGoodDescIdDesc(Pageable pageable);
    List<Post> findAllByOrderByScrapDescIdDesc(Pageable pageable);
    long count();
    List<Post> findAllByMemberOrderByIdDesc(Member member,Pageable pageable);
    List<Post> findAllByMemberOrderByGoodDescIdDesc(Member member,Pageable pageable);
    List<Post> findAllByMemberOrderByScrapDescIdDesc(Member member,Pageable pageable);
    List<Post> findAllByTitleContainsOrContentContainsOrderByIdDesc(String title,String content,Pageable pageable);
    List<Post> findAllByTitleContainsOrContentContainsOrderByGoodDescIdDesc(String title,String content,Pageable pageable);
    List<Post> findAllByTitleContainsOrContentContainsOrderByScrapDescIdDesc(String title,String content,Pageable pageable);
    Long countAllByTitleContainsOrContentContains(String title,String content);
    @Query("SELECT p FROM Post p WHERE p.good >= 1 AND (:category IS NULL OR p.category = :category) ORDER BY p.good DESC ,p.id DESC LIMIT 12")
    List<Post> findTop12(String category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Post p WHERE p.id =:id")
    Optional<Post> findByIdWithLock(Long id);



}
