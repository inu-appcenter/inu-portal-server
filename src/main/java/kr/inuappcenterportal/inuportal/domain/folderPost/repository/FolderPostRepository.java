package kr.inuappcenterportal.inuportal.domain.folderPost.repository;

import kr.inuappcenterportal.inuportal.domain.folder.model.Folder;
import kr.inuappcenterportal.inuportal.domain.folderPost.model.FolderPost;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FolderPostRepository extends JpaRepository<FolderPost,Long> {
    Optional<FolderPost> findByFolderAndPost(Folder folder, Post post);
    boolean existsByFolderAndPost(Folder folder, Post post);
    @Query("SELECT f FROM FolderPost f JOIN FETCH f.post p WHERE f.folder=:folder")
    List<FolderPost> findAllByFolder(Folder folder, Sort sort);
    /*@Query("SELECT f FROM FolderPost f JOIN FETCH f.post p WHERE f.folder=:folder ORDER BY p.id desc")
    List<FolderPost> findAllByFolder(Folder folder);
    @Query("SELECT f FROM FolderPost f JOIN FETCH f.post p WHERE f.folder=:folder ORDER BY p.good desc")
    List<FolderPost> findAllByFolderOrderByGood(Folder folder);
    @Query("SELECT f FROM FolderPost f JOIN FETCH f.post p WHERE f.folder=:folder ORDER BY p.scrap desc")
    List<FolderPost> findAllByFolderOrderByScrap(Folder folder);*/
    @Query("SELECT f FROM FolderPost f JOIN FETCH f.post p WHERE f.folder=:folder and (f.post.content LIKE concat('%',:content,'%') or f.post.title LIKE concat('%',:content,'%'))")
    List<FolderPost> searchInFolder(Folder folder,String content,Sort sort);
    /*@Query("SELECT f FROM FolderPost f JOIN FETCH f.post p WHERE f.folder=:folder and (f.post.content LIKE concat('%',:content,'%') or f.post.title LIKE concat('%',:content,'%')) ORDER BY p.good desc ")
    List<FolderPost> searchInFolderOrderByGood(Folder folder,String content);
    @Query("SELECT f FROM FolderPost f JOIN FETCH f.post p WHERE f.folder=:folder and (f.post.content LIKE concat('%',:content,'%') or f.post.title LIKE concat('%',:content,'%')) ORDER BY p.scrap desc ")
    List<FolderPost> searchInFolderOrderByScrap(Folder folder,String content);*/
}
