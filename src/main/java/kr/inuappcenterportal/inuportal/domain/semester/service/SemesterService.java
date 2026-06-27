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
@Transactional
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
     * 올해, 내년 일정 가져오는 메서드
     */
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
        Optional<Schedule> summerSemesterStart = findScheduleContaining(semesterStartCandidates, "여름");
        Optional<Schedule> winterSemesterStart = findScheduleContaining(semesterStartCandidates, "겨울");


        // 학기 생성
        createSemester(year, SemesterTerm.FIRST, firstSemesterStart);
        createSemester(year, SemesterTerm.SECOND, secondSemesterStart);
        createSemester(year, SemesterTerm.SUMMER, summerSemesterStart);
        createSemester(year, SemesterTerm.WINTER, winterSemesterStart);
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
    private void createSemester(Integer year, SemesterTerm term, Optional<Schedule> startSchedule) {
        if (startSchedule.isEmpty()) {
            return;
        }

        boolean alreadyExists = semesterRepository.findByYearAndTerm(year, term).isPresent();

        if (alreadyExists) {
            return;
        }

        Semester semester = Semester.create(
                year,
                term,
                startSchedule.get().getStartDate(),
                null
        );

        semesterRepository.save(semester);
    }
}
