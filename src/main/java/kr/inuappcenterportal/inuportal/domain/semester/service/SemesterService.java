package kr.inuappcenterportal.inuportal.domain.semester.service;

import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.semester.dto.SemesterResponseDto;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final ScheduleRepository scheduleRepository;
    private final Clock clock;

    /**
     * 만들어진 학기 조회 메서드
     */
    @Transactional(readOnly = true)
    public List<SemesterResponseDto> getValidSemesters() {
        return semesterRepository.findAllByStatusInOrderByYearDescTermAsc(
                        List.of(SemesterStatus.OPEN, SemesterStatus.CLOSED)
                )
                .stream()
                .map(SemesterResponseDto::from)
                .toList();
    }

    /**
     * 년도 동기화 메서드
     */
    @Transactional
    public void syncSemestersByYear() {
        int year = LocalDate.now(clock).getYear();

        syncSemesters(year);
    }

    /**
     * 각 학기의 시작일 동기화 메서드
     */
    private void syncSemesters(int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<Schedule> semesterStartCandidates =
                scheduleRepository.findAcademicSemesterSchedules(
                        yearStart,
                        yearEnd,
                        "개강",
                        "계절학기"
                );

        // 학사일정 크롤링한 데이터에서 파싱
        Optional<Schedule> firstSemesterStart = findScheduleContaining(semesterStartCandidates, "1학기");
        Optional<Schedule> secondSemesterStart = findScheduleContaining(semesterStartCandidates, "2학기");
        Optional<Schedule> summerSemesterStart = findScheduleContaining(semesterStartCandidates, "하계");
        Optional<Schedule> winterSemesterStart = findScheduleContaining(semesterStartCandidates, "동계");

        // 학기 생성
        createSemester(year, SemesterTerm.FIRST, firstSemesterStart, summerSemesterStart, winterSemesterStart);
        createSemester(year, SemesterTerm.SECOND, secondSemesterStart, summerSemesterStart, winterSemesterStart);
        createSemester(year, SemesterTerm.SUMMER, summerSemesterStart, summerSemesterStart, winterSemesterStart);
        createSemester(year, SemesterTerm.WINTER, winterSemesterStart, summerSemesterStart, winterSemesterStart);
    }

    // 파싱한 데이터에서 키워드로 찾기
    private Optional<Schedule> findScheduleContaining(List<Schedule> schedules, String keyword) {
        return schedules.stream()
                .filter(schedule -> schedule.getContent() != null)
                .filter(schedule -> schedule.getContent().contains(keyword))
                .findFirst();
    }


    /**
     * 학기 생성 및 저장 메서드
     */
    private void createSemester(
            Integer year,
            SemesterTerm term,
            Optional<Schedule> startSchedule,
            Optional<Schedule> summerSemesterStart,
            Optional<Schedule> winterSemesterStart
    ) {
        if (startSchedule.isEmpty()) {
            return;
        }

        boolean alreadyExists = semesterRepository.findByYearAndTerm(year, term).isPresent();

        if (alreadyExists) {
            return;
        }

        LocalDate startDate = startSchedule.get().getStartDate(); // 학기 시작일

        LocalDate endDate = calculateEndDate( // 학기 종료일
                term,
                startDate,
                summerSemesterStart,
                winterSemesterStart
        );

        if (endDate == null) {
            return;
        }

        SemesterStatus status = calculateStatus(startDate, endDate, LocalDate.now(clock)); // 학기 상태 계산


        Semester semester = Semester.create(
                year,
                term,
                status,
                startDate,
                endDate
        );

        semesterRepository.save(semester);
    }

    /**
     * 각 학기의 종료일 계산 메서드
     */
    private LocalDate calculateEndDate(
            SemesterTerm term,
            LocalDate startDate,
            Optional<Schedule> summerSemesterStart,
            Optional<Schedule> winterSemesterStart
    ) {
        // 1학기 종료일은 여름 계절학기 시작 하루 전
        if (term == SemesterTerm.FIRST) {
            return summerSemesterStart
                    .map(schedule -> schedule.getStartDate().minusDays(1))
                    .orElse(null);
        }

        // 2학기 종료일은 겨울 계절학기 시작 하루 전
        if (term == SemesterTerm.SECOND) {
            return winterSemesterStart
                    .map(schedule -> schedule.getStartDate().minusDays(1))
                    .orElse(null);
        }

        // 각 계절학기는 정확히 3주, 21일간 진행
        if (term == SemesterTerm.SUMMER || term == SemesterTerm.WINTER) {
            return startDate.plusDays(20);
        }

        return null;
    }

    /**
     * 학기 상태 변경 메서드
     */
    @Transactional
    public void updateSemesterStatus() {
        LocalDate today = LocalDate.now(clock);

        List<Semester> semesters = semesterRepository.findAll();

        for (Semester semester : semesters) {
            SemesterStatus status = calculateStatus(
                    semester.getStartDate(),
                    semester.getEndDate(),
                    today
            );

            semester.updateStatus(status);
        }
    }

    /**
     * 학기 상태 계산 메서드
     */
    private SemesterStatus calculateStatus(LocalDate startDate, LocalDate endDate, LocalDate today) {

        // 학기 시작일의 4주 전부터 학기가 열림
        LocalDate openDate = startDate.minusWeeks(4);

        // 학기 시작일의 4주 전보다 이전이면 UPCOMING
        if (today.isBefore(openDate)) {
            return SemesterStatus.UPCOMING;
        }

        // 종료일 이후면 CLOSED
        if (endDate != null && today.isAfter(endDate)) {
            return SemesterStatus.CLOSED;
        }

        // 그 이외에는 OPEN
        return SemesterStatus.OPEN;
    }
}
