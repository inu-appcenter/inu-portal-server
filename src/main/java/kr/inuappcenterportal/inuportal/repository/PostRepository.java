package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {
    List<Post> findAllByCategory (String category);
    List<Post> findAllByMember(Member member);
    List<Post> findAllByTitleContainsOrderByCreateDateDesc(String title);
    List<Post> findAllByTitleContainsOrderByViewDesc(String title);
    List<Post> findAllByTitleContains(String title);

    List<Post> findAllByContentContainsOrderByCreateDateDesc(String content);
    List<Post> findAllByContentContainsOrderByViewDesc(String content);
    List<Post> findAllByContentContains(String content);
}
