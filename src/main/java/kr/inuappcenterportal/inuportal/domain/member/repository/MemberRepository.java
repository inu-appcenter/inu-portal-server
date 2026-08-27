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
    Optional<Member> findByNickname(String nickname);
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

    @Query("""
            SELECT m.id
            FROM Member m
            WHERE m.id NOT IN (
                SELECT DISTINCT t.member.id
                FROM TimeTable t
                WHERE t.semester.id = :semesterId
            )
            """)
    List<Long> findIdsWithoutTimeTableForSemester(@Param("semesterId") Long semesterId);

    @Query("""
            SELECT DISTINCT t.member.id
            FROM TimeTable t
            WHERE t.semester.id = :semesterId
            AND t.id NOT IN (
                SELECT DISTINCT ti.timeTable.id
                FROM TimeTableItem ti
            )
            """)
    List<Long> findIdsWithEmptyTimeTableForSemester(@Param("semesterId") Long semesterId);

    @Query("""
            SELECT DISTINCT t.member.id
            FROM TimeTable t
            WHERE t.semester.id = :pastSemesterId
            AND t.member.id NOT IN (
                SELECT DISTINCT t2.member.id
                FROM TimeTable t2
                WHERE t2.semester.id = :currentSemesterId
            )
            """)
    List<Long> findIdsWithPastTimeTableButNoCurrentSemester(@Param("currentSemesterId") Long currentSemesterId, @Param("pastSemesterId") Long pastSemesterId);

    @Query("""
            SELECT m.id
            FROM Member m
            WHERE m.id NOT IN (
                SELECT f.requester.id FROM Friend f WHERE f.status = kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus.ACCEPTED
            )
            AND m.id NOT IN (
                SELECT f.receiver.id FROM Friend f WHERE f.status = kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus.ACCEPTED
            )
            """)
    List<Long> findIdsWithNoFriends();

    @Query("""
            SELECT m.id
            FROM Member m
            WHERE m.id NOT IN (
                SELECT DISTINCT p.member.id FROM Post p WHERE p.member.id IS NOT NULL
            )
            AND m.id NOT IN (
                SELECT DISTINCT r.member.id FROM Reply r WHERE r.member.id IS NOT NULL
            )
            """)
    List<Long> findIdsWithNoCommunityActivity();

}
