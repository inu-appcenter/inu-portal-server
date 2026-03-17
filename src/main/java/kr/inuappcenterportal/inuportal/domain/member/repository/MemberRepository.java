package kr.inuappcenterportal.inuportal.domain.member.repository;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {

    //boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    Optional<Member> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);

    @Query("SELECT m.id FROM Member m")
    List<Long> findAllIds();

    @Query("SELECT DISTINCT f.memberId FROM FcmToken f WHERE f.memberId IS NOT NULL")
    List<Long> findIdsWithLinkedFcmToken();

    @Query("""
            SELECT m.id
            FROM Member m
            WHERE m.id NOT IN (
                SELECT DISTINCT f.memberId
                FROM FcmToken f
                WHERE f.memberId IS NOT NULL
            )
            """)
    List<Long> findIdsWithoutLinkedFcmToken();

    @Query("SELECT m.id FROM Member m WHERE m.studentId IN :studentIds")
    List<Long> findIdsByStudentIdIn(@Param("studentIds") List<String> studentIds);

    @Query("SELECT m.id FROM Member m WHERE m.department IN :departments")
    List<Long> findIdsByDepartmentIn(@Param("departments") List<Department> departments);

}
