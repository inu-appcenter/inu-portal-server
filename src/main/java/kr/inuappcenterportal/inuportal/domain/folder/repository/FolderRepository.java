package kr.inuappcenterportal.inuportal.domain.folder.repository;

import kr.inuappcenterportal.inuportal.domain.folder.model.Folder;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder,Long> {
    List<Folder> findAllByMember(Member member);

    // 회원탈퇴 시 해당 회원의 데이터를 한 번에 지운다.
    @Modifying(flushAutomatically = true)
    @Query("delete from Folder f where f.member.id = :memberId")
    int deleteAllByMemberIdForWithdrawal(@Param("memberId") Long memberId);
}
