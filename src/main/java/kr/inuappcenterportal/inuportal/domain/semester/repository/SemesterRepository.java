package kr.inuappcenterportal.inuportal.domain.semester.repository;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    /// 년도와 term으로 학기 찾기
    Optional<Semester> findByYearAndTerm(Integer year, SemesterTerm term);

    /// status가 주어진 목록 안에 포함되는 Semester를 찾고 year 내림차순, term 오름차순으로 정렬
    List<Semester> findAllByStatusInOrderByYearDescTermAsc(List<SemesterStatus> statuses);
}
