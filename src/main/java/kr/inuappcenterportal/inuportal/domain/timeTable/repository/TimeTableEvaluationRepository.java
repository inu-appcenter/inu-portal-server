package kr.inuappcenterportal.inuportal.domain.timeTable.repository;

import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TimeTableEvaluationRepository extends JpaRepository<TimeTableEvaluation, Long> {

    Optional<TimeTableEvaluation> findByTimeTableId(Long timeTableId);

    void deleteByTimeTableId(Long timeTableId);
}
