package kr.inuappcenterportal.inuportal.domain.member.repository;

import kr.inuappcenterportal.inuportal.domain.member.model.GradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradeRecordRepository extends JpaRepository<GradeRecord, Long> {
    List<GradeRecord> findAllByMemberIdAndSemesterId(Long memberId, Long semesterId);

    void deleteAllByMemberIdAndSemesterId(Long memberId, Long semesterId);

    Optional<GradeRecord> findByIdAndMemberId(Long id, Long memberId);
}
