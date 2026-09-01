package kr.inuappcenterportal.inuportal.domain.semester.repository;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    // 년도와 term으로 학기 찾기
    Optional<Semester> findByYearAndTerm(Integer year, SemesterTerm term);

    Optional<Semester> findFirstByStatusOrderByStartDateDesc(SemesterStatus status);

    List<Semester> findAllByOrderByStartDateDesc();
    List<Semester> findAllByStatus(SemesterStatus status);

    // 특정 연도 이후 학기만 조회 (오래된 과거 학기는 목록에서 제외)
    List<Semester> findAllByYearGreaterThanEqual(Integer year);
}
