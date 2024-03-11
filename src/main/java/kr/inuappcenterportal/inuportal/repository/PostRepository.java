package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {
    List<Post> findAllByCategoryOrderByIdDesc (String category,Pageable pageable);
    List<Post> findAllByOrderByIdDesc(Pageable pageable);
    List<Post> findAllByMemberOrderByIdDesc(Member member,Pageable pageable);
    List<Post> findAllByTitleContainsOrContentContainsOrderByIdDesc(String title,String content,Pageable pageable);
    List<Post> findAllByTitleContainsOrContentContainsOrderByViewDesc(String title,String content,Pageable pageable);
    List<Post> findAllByTitleContainsOrContentContains(String title,String content,Pageable pageable);

}
