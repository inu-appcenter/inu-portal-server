package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Folder;
import kr.inuappcenterportal.inuportal.domain.FolderPost;
import kr.inuappcenterportal.inuportal.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FolderPostRepository extends JpaRepository<FolderPost,Long> {
    Optional<FolderPost> findByFolderAndPost(Folder folder, Post post);
    boolean existsByFolderAndPost(Folder folder, Post post);
    List<FolderPost> findAllByFolder(Folder folder);
    @Query("SELECT f FROM FolderPost f JOIN FETCH f.post WHERE f.folder=:folder and (f.post.content LIKE concat('%',:content,'%') or f.post.title LIKE concat('%',:content,'%'))")
    List<FolderPost> searchInFolder(Folder folder,String content);
}
