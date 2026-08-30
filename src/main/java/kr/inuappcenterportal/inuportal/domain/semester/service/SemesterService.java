package kr.inuappcenterportal.inuportal.domain.semester.service;

import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.semester.dto.SemesterResponseDto;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemesterService {

    // 이보다 이전 연도의 학기는 오래된 데이터라 목록 조회에서 제외
    private static final int MIN_VISIBLE_YEAR = 2020;
    private final SemesterRepository semesterRepository;
    private final ScheduleRepository scheduleRepository;
    private final Clock clock;

    /**
     * 만들어진 학기 조회 메서드
     */
    @Transactional(readOnly = true)
    public List<SemesterResponseDto> getSemesters() {
        return semesterRepository.findAllByYearGreaterThanEqual(MIN_VISIBLE_YEAR)
                .stream()
                .sorted(Comparator
                        .comparingInt((Semester semester) -> statusOrder(semester.getStatus()))
                        .thenComparing(Semester::getYear, Comparator.reverseOrder())
                        .thenComparingInt(semester -> termOrderDescending(semester.getTerm())))
                .map(SemesterResponseDto::from)
                .toList();
    }

    // 상태 기준 정렬
    private int statusOrder(SemesterStatus status) {
        return switch (status) {
            case OPEN -> 0;
            case CLOSED -> 1;
            case UPCOMING -> 2;
        };
    }

    // 학기 기준 정렬
    private int termOrderDescending(SemesterTerm term) {
        return switch (term) {
            case WINTER -> 1;
            case SECOND -> 2;
            case SUMMER -> 3;
            case FIRST -> 4;
        };
    }

    /**
     * 년도 동기화 메서드
     */
    @Transactional
    public void syncSemestersByYear() {
        int year = LocalDate.now(clock).getYear();

        syncSemesters(year);
        log.info("학기 동기화 성공");
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
            Optional<Schedule> semesterSchedule,
            Optional<Schedule> summerSemesterStart,
            Optional<Schedule> winterSemesterStart
    ) {
        if (semesterSchedule.isEmpty()) {
            return;
        }

        LocalDate startDate = semesterSchedule.get().getStartDate(); // 학기 시작일

        LocalDate endDate = calculateEndDate( // 학기 종료일
                term,
                semesterSchedule,
                summerSemesterStart,
                winterSemesterStart
        );

        if (endDate == null) {
            return;
        }

        // 학기 상태 계산
        SemesterStatus status = calculateStatus(startDate, endDate, LocalDate.now(clock));


        // 정규학기 종료일은 계절학기 시작일 기준으로 계산
        // 계절학기 종료일은 학사일정의 endDate 그대로 사용
        semesterRepository.findByYearAndTerm(year, term)
                .ifPresentOrElse(
                        semester -> semester.updatePeriodAndStatus(startDate, endDate, status),
                        () -> semesterRepository.save(
                                Semester.create(year, term, status, startDate, endDate)
                        )
                );
    }

    /**
     * 각 학기의 종료일 계산 메서드
     */
    private LocalDate calculateEndDate(
            SemesterTerm term,
            Optional<Schedule> semesterSchedule,
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

        // 계절학기는 학사일정에 저장된 종료일을 그대로 사용
        if (term == SemesterTerm.SUMMER || term == SemesterTerm.WINTER) {
            return semesterSchedule
                    .map(Schedule::getEndDate)
                    .orElse(null);
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
