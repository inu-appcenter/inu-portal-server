package kr.inuappcenterportal.inuportal.domain.timeTable.repository;

import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimeTableRepository extends JpaRepository<TimeTable, Long> {
    boolean existsByMemberIdAndSemesterIdAndTimeTableName(Long memberId, Long semesterId, String timetableName);

    List<TimeTable> findAllByMemberIdAndSemesterId(Long memberId, Long semesterId);

    boolean existsByMemberIdAndSemesterId(Long memberId, Long semesterId);

    Optional<TimeTable> findByMemberIdAndSemesterIdAndIsPrimaryTrue(Long memberId, Long semesterId);

    boolean existsByMemberIdAndSemesterIdAndTimeTableNameAndIdNot(
            Long memberId,
            Long semesterId,
            String timeTableName,
            Long timeTableId
    );
}
