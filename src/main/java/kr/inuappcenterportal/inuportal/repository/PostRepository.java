package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {
    List<Post> findAllByCategoryOrderByIdDesc (String category,Pageable pageable);
    Long countAllByCategory (String category);
    List<Post> findAllByOrderByIdDesc(Pageable pageable);
    long count();
    List<Post> findAllByMemberOrderByIdDesc(Member member,Pageable pageable);
    List<Post> findAllByTitleContainsOrContentContainsOrderByIdDesc(String title,String content,Pageable pageable);
    Long countAllByTitleContainsOrContentContains(String title,String content);

    List<Post> findAllByTitleContainsOrContentContains(String title,String content,Pageable pageable);


}
