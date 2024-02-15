package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Folder;
import kr.inuappcenterportal.inuportal.domain.FolderPost;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderPostRepository extends JpaRepository<FolderPost,Long> {
    Optional<FolderPost> findByFolderAndPost(Folder folder, Post post);
    boolean existsByFolderAndPost(Folder folder, Post post);
    List<FolderPost> findAllByFolder(Folder folder);
}
