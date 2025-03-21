package kr.inuappcenterportal.inuportal.domain.schedule.repository;

import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule,Long> {
    @Query("SELECT s FROM Schedule s WHERE (YEAR(s.startDate) = :year AND MONTH(s.startDate) = :month) OR (YEAR(s.endDate) = :year AND MONTH(s.endDate) = :month) ORDER BY s.startDate ASC")
    List<Schedule> findAllByStartDateOrEndDateMonth(int year, int month);

    boolean existsByContent(String content);


}
