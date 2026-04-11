package kr.inuappcenterportal.inuportal.domain.schedule.repository;

import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule,Long> {
    @Query("""
            SELECT s
            FROM Schedule s
            WHERE s.aiGenerated = false
              AND s.department IS NULL
              AND (
                    (YEAR(s.startDate) = :year AND MONTH(s.startDate) = :month)
                    OR (YEAR(s.endDate) = :year AND MONTH(s.endDate) = :month)
              )
            ORDER BY s.startDate ASC
            """)
    List<Schedule> findAllByStartDateOrEndDateMonth(int year, int month);

    boolean existsByContentAndStartDateAndEndDateAndDepartmentIsNullAndAiGeneratedFalse(String content, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(MAX(s.id), 0) FROM Schedule s")
    Long findMaxId();

    void deleteBySourceNoticeIdAndAiGeneratedTrue(Long sourceNoticeId);

    boolean existsBySourceNoticeIdAndContentAndStartDateAndEndDateAndAiGeneratedTrue(
            Long sourceNoticeId,
            String content,
            LocalDate startDate,
            LocalDate endDate
    );
}
