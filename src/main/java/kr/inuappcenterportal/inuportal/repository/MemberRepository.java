package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {

    //boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    Optional<Member> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);

}
