package kr.inuappcenterportal.inuportal.domain.member.repository;

import kr.inuappcenterportal.inuportal.domain.member.model.GradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GradeRecordRepository extends JpaRepository<GradeRecord, Long> {
    List<GradeRecord> findAllByMemberIdAndSemesterId(Long memberId, Long semesterId);

    List<GradeRecord> findAllByMemberId(Long memberId);

    /**
     * 벌크 DELETE. 파생 delete 는 영속성 컨텍스트에 remove 표시만 하고 flush 시점에 나가는데,
     * Hibernate ActionQueue 가 INSERT 를 DELETE 보다 먼저 실행하므로
     * replaceGradeRecord 의 "삭제 후 재삽입" 이 unique 제약에 걸린다.
     * 벌크 DELETE 는 호출 즉시 SQL 이 실행되어 순서가 보장된다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from grade_record where member_id = :memberId and semester_id = :semesterId", nativeQuery = true)
    void deleteAllByMemberIdAndSemesterId(@Param("memberId") Long memberId, @Param("semesterId") Long semesterId);

    Optional<GradeRecord> findByIdAndMemberId(Long id, Long memberId);
}
