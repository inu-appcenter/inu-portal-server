package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {
    List<Post> findAllByCategory (String category);
    List<Post> findAllByMember(Member member);
    List<Post> findAllByTitleContainsOrContentContainsOrderByCreateDateDesc(String title,String content);
    List<Post> findAllByTitleContainsOrContentContainsOrderByViewDesc(String title,String content);
    List<Post> findAllByTitleContainsOrContentContains(String title,String content);

}
