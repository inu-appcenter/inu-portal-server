package kr.inuappcenterportal.inuportal.domain.semester.service;

import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final ScheduleRepository scheduleRepository;

    /**
     * 만들어진 학기 조회 메서드
     */
    @Transactional(readOnly = true)
    public Semester getSemester(Integer year, SemesterTerm term) {
        return semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 학기입니다."
                ));
    }

    /**
     * 년도 동기화 메서드
     */
    @Transactional
    public void syncSemestersByYear() {
        int year = LocalDate.now().getYear();

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
        //
        if (startSchedule.isEmpty()) {
            return;
        }

        boolean alreadyExists = semesterRepository.findByYearAndTerm(year, term).isPresent();

        if (alreadyExists) {
            return;
        }

        //
        LocalDate startDate = startSchedule.get().getStartDate();
        LocalDate endDate = calculateEndDate(
                term,
                startDate,
                summerSemesterStart,
                winterSemesterStart
        );

        Semester semester = Semester.create(
                year,
                term,
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

        // 2하긱 종료일은 겨울 게절학기 시작 하루 전
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
}
