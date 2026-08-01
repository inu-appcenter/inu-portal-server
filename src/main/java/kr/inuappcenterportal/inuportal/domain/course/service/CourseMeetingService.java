package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseMeetingApiItem;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseMeetingGroupKey;
import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseMeetingService {

    private final SemesterRepository semesterRepository;
    private final CourseMeetingRepository courseMeetingRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    /**
     * 개설 강의 시간 정보 동기화 메서드
     */
    @Transactional
    public void upsertCourseMeetings(
            Map<CourseMeetingGroupKey, List<CourseMeetingApiItem>> groupedByCourseOffering) {
        // API 응답을 CourseOffering별로 묶어서, 해당 개설강의의 시간표 정보를 DB에 새로 저장하는 로직
        for (Map.Entry<CourseMeetingGroupKey, List<CourseMeetingApiItem>> entry : groupedByCourseOffering.entrySet()) {
            CourseMeetingGroupKey key = entry.getKey();
            List<CourseMeetingApiItem> meetingItems = entry.getValue();

            // API의 YEAR, TERM_CODE로 내부 DB의 Semester 찾기
            Semester semester = semesterRepository.findByYearAndTerm(
                    Integer.parseInt(key.year()),
                    SemesterTerm.mapToTermCode(key.termCode())
            ).orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

            // 찾은 학기와 학수코드로 개설 강의 찾기
            CourseOffering courseOffering = courseOfferingRepository
                    .findBySemesterIdAndSubjectNumber(semester.getId(), key.haksuCode())
                    .orElse(null);

            if (courseOffering == null) {
                log.warn("강의 시간 정보에 해당하는 개설 강의를 찾을 수 없습니다. year={}, termCode={}, haksuCode={}",
                        key.year(), key.termCode(), key.haksuCode());
                continue;
            }

            // 업데이트가 될 떄 기존 시간 정보를 지웠다가 다시 생성(이게 더 안전하고 효율적, 데이터가 적어서)
            courseMeetingRepository.deleteAllByCourseOfferingId(courseOffering.getId());

            // API로 받아온 값은 CourseOffering 필드에 맞게 변환
            List<CourseMeeting> meetings = meetingItems.stream()
                    .map(item -> CourseMeeting.create(
                            courseOffering,
                            item.roomName(),
                            item.lectmName(),
                            DayOfWeek.mapDay(item.dayName()),
                            LocalTime.parse(item.lectmStart()),
                            LocalTime.parse(item.lectmEnd())
                    )).toList();

            courseMeetingRepository.saveAll(meetings);
        }
    }
}
