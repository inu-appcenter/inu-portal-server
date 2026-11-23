package kr.inuappcenterportal.inuportal.domain.member.repository;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {

    //boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    Optional<Member> findByNickname(String nickname);
    // 탈퇴 회원은 모두 닉네임이 '탈퇴한 사용자'라 겹친다. 닉네임으로 다른 회원을 찾을 때는 이걸 쓴다.
    Optional<Member> findByNicknameAndDeletedAtIsNull(String nickname);

    // 회원탈퇴 동시 요청을 한 번만 처리하려고 행을 잠그고 읽는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT m FROM Member m WHERE m.deletedAt IS NULL")
    List<Member> findAllActive();
    Optional<Member> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
    Optional<Member> findByExamNo(String examNo);

    @Query("SELECT m FROM Member m WHERE m.memberType = kr.inuappcenterportal.inuportal.domain.member.enums.MemberType.FRESHMAN AND m.freshmanVerifyHash = :verifyHash")
    Optional<Member> findFreshmanByVerifyHash(@Param("verifyHash") String verifyHash);

    @Query("""
            SELECT m
            FROM Member m
            WHERE m.nearbyVisibility = true
              AND m.locationUpdatedAt >= :cutoffTime
              AND m.id != :requesterId
              AND m.latitude IS NOT NULL
              AND m.longitude IS NOT NULL
              AND m.deletedAt IS NULL
            """)
    List<Member> findActiveNearbyCandidates(@Param("cutoffTime") LocalDateTime cutoffTime, @Param("requesterId") Long requesterId);

    // 알림 대상 조회(findAllIds, find...)는 탈퇴 회원을 뺀다.
    @Query("SELECT m.id FROM Member m WHERE m.deletedAt IS NULL")
    List<Long> findAllIds();

    @Query("SELECT DISTINCT f.memberId FROM FcmToken f WHERE f.memberId IS NOT NULL")
    List<Long> findIdsWithLinkedFcmToken();

    @Query("""
            SELECT m.id
            FROM Member m
            WHERE m.deletedAt IS NULL
            AND m.id NOT IN (
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
            WHERE m.deletedAt IS NULL
            AND m.id NOT IN (
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
            WHERE m.deletedAt IS NULL
            AND m.id NOT IN (
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
            WHERE m.deletedAt IS NULL
            AND m.id NOT IN (
                SELECT DISTINCT p.member.id FROM Post p WHERE p.member.id IS NOT NULL
            )
            AND m.id NOT IN (
                SELECT DISTINCT r.member.id FROM Reply r WHERE r.member.id IS NOT NULL
            )
            """)
    List<Long> findIdsWithNoCommunityActivity();

}
