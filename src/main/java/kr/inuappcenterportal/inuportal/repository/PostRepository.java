package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post,Long> {
}
