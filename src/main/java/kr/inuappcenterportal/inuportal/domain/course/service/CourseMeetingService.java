package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseMeetingApiItem;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseMeetingGroupKey;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
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

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

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
            Map<CourseMeetingGroupKey, List<CourseMeetingApiItem>> groupedByCourseOffering
    ) {
        // API 응답을 CourseOffering별로 묶어서, 해당 개설강의의 시간표 정보를 DB에 새로 저장하는 로직
        for (Map.Entry<CourseMeetingGroupKey, List<CourseMeetingApiItem>> entry : groupedByCourseOffering.entrySet()) {
            try {
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
                                item.lectmCode(),
                                DayOfWeek.mapDay(item.dayName()),
                                LocalTime.parse(item.lectmStart()),
                                LocalTime.parse(item.lectmEnd())
                        )).toList();

                courseMeetingRepository.saveAll(meetings);

            } catch (Exception e) {
                CourseMeetingGroupKey key = entry.getKey();
                log.warn("강의 시간 동기화 스킵. year={}, termCode={}, haksuCode={}, reason={}",
                        key.year(), key.termCode(), key.haksuCode(), e.getMessage());
            }
        }
    }

    /**
     * 연강 판단 후 meetings 합치는 메서드
     */
    public List<CourseMeetingResponseDto> mergeContinuousMeetings(List<CourseMeeting> meetings) {
        if (meetings == null || meetings.isEmpty()) {
            return List.of();
        }

        // day->location->startTime 순으로 정렬
        List<CourseMeetingResponseDto> sortedMeetings = meetings.stream()
                .map(CourseMeetingResponseDto::from)
                .sorted(Comparator
                        .comparing(CourseMeetingResponseDto::day)
                        .thenComparing(CourseMeetingResponseDto::location, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(CourseMeetingResponseDto::startTime))
                .toList();

        List<CourseMeetingResponseDto> result = new ArrayList<>();
        CourseMeetingResponseDto current = sortedMeetings.get(0); // 정렬된 meetings의 첫번째 원소

        for (int i = 1; i < sortedMeetings.size(); i++) {
            CourseMeetingResponseDto next = sortedMeetings.get(i);

            // current의 다음 원소부터 비교하면서 머지가 가능한지 확인
            // current에 계속 중첩됨.
            if (canMerge(current, next)) {
                current = merge(current, next);
            } else {
                result.add(current);
                current = next;
            }
        }

        result.add(current);
        return result;
    }

    // 합치려는 두 meeting 객체가 연강인지 판단하는 메서드
    private boolean canMerge(CourseMeetingResponseDto current, CourseMeetingResponseDto next) {

        // 1. 요일이 같은지 확인
        if (!Objects.equals(current.day(), next.day())) {
            return false;
        }

        // 2. 강의실 같은지 확인
        if (!Objects.equals(current.location(), next.location())) {
            return false;
        }

        // 3. 시간이 있는지 확인
        if (current.endTime() == null || next.startTime() == null || next.endTime() == null) {
            return false;
        }

        //
        if (next.startTime().isBefore(current.endTime())) {
            return !next.endTime().isBefore(current.endTime());
        }

        // 10분 내외일때만 연강
        long gapMinutes = Duration.between(current.endTime(), next.startTime()).toMinutes();
        return gapMinutes <= 15;
    }

    // 연강이면 합치는 메서드
    private CourseMeetingResponseDto merge(
            CourseMeetingResponseDto current,
            CourseMeetingResponseDto next
    ) {
        return new CourseMeetingResponseDto(
                current.id(),
                current.location(),
                mergeSequence(current.sequence(), next.sequence()),
                mergeLectmCodes(current.lectmCode(), next.lectmCode()),
                current.day(),
                current.startTime(),
                next.endTime().isAfter(current.endTime()) ? next.endTime() : current.endTime()
        );
    }

    private String mergeSequence(String currentSequence, String nextSequence) {
        if (currentSequence == null || currentSequence.isBlank()) {
            return nextSequence;
        }

        if (nextSequence == null || nextSequence.isBlank()) {
            return currentSequence;
        }

        if (currentSequence.contains(nextSequence)) {
            return currentSequence;
        }

        return currentSequence + "," + nextSequence;
    }

    private String mergeLectmCodes(String currentCode, String nextCode) {
        if (currentCode == null || currentCode.isBlank()) return nextCode;
        if (nextCode == null || nextCode.isBlank() || currentCode.contains(nextCode)) return currentCode;
        return currentCode + "," + nextCode;
    }
}
