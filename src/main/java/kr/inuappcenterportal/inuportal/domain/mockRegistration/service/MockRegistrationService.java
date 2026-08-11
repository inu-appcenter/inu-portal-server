package kr.inuappcenterportal.inuportal.domain.mockRegistration.service;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseMeetingService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.dto.TimetableImportResponseDto;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.model.MockEnrollment;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.model.MockWatchlistItem;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.repository.MockEnrollmentRepository;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.repository.MockWatchlistRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.TimeTableItemType;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MockRegistrationService {
    private static final int WATCHLIST_LIMIT = 10;
    private static final int CREDIT_LIMIT = 24;
    private final SemesterRepository semesterRepository;
    private final CourseOfferingRepository offeringRepository;
    private final CourseMeetingRepository meetingRepository;
    private final CourseMeetingService meetingService;
    private final MockWatchlistRepository watchlistRepository;
    private final MockEnrollmentRepository enrollmentRepository;
    private final TimeTableRepository timeTableRepository;
    private final TimeTableItemRepository timeTableItemRepository;

    public List<CourseOfferingResponseDto> getWatchlist(Member member) {
        Semester semester = openSemester();
        return toResponses(watchlistRepository.findAllByMemberIdAndSemesterId(member.getId(), semester.getId()).stream()
                .map(MockWatchlistItem::getCourseOffering).toList());
    }

    public List<CourseOfferingResponseDto> getEnrollments(Member member) {
        Semester semester = openSemester();
        return toResponses(enrollmentRepository.findAllByMemberIdAndSemesterId(member.getId(), semester.getId()).stream()
                .map(MockEnrollment::getCourseOffering).toList());
    }

    @Transactional
    public void addWatchlist(Member member, Long offeringId) {
        Semester semester = openSemester();
        CourseOffering offering = offering(semester, offeringId);
        if (watchlistRepository.existsByMemberIdAndSemesterIdAndCourseOfferingId(member.getId(), semester.getId(), offeringId))
            throw new MyException(MyErrorCode.MOCK_WATCHLIST_DUPLICATE);
        if (watchlistRepository.countByMemberIdAndSemesterId(member.getId(), semester.getId()) >= WATCHLIST_LIMIT)
            throw new MyException(MyErrorCode.MOCK_WATCHLIST_LIMIT);
        List<CourseOffering> existing = watchlistRepository.findAllByMemberIdAndSemesterId(member.getId(), semester.getId()).stream()
                .map(MockWatchlistItem::getCourseOffering).toList();
        validateNoConflict(existing, offering);
        watchlistRepository.save(MockWatchlistItem.create(member, semester, offering));
    }

    @Transactional
    public void removeWatchlist(Member member, Long offeringId) {
        Semester semester = openSemester();
        MockWatchlistItem item = watchlistRepository.findByMemberIdAndSemesterIdAndCourseOfferingId(member.getId(), semester.getId(), offeringId)
                .orElseThrow(() -> new MyException(MyErrorCode.MOCK_WATCHLIST_NOT_FOUND));
        watchlistRepository.delete(item);
    }

    @Transactional
    public void enroll(Member member, Long offeringId) {
        Semester semester = openSemester();
        CourseOffering offering = offering(semester, offeringId);
        if (enrollmentRepository.existsByMemberIdAndSemesterIdAndCourseOfferingId(member.getId(), semester.getId(), offeringId))
            throw new MyException(MyErrorCode.MOCK_ENROLLMENT_DUPLICATE);
        List<CourseOffering> existing = enrollmentRepository.findAllByMemberIdAndSemesterId(member.getId(), semester.getId()).stream()
                .map(MockEnrollment::getCourseOffering).toList();
        if (existing.stream().anyMatch(item -> sameCourse(item, offering)))
            throw new MyException(MyErrorCode.MOCK_ENROLLMENT_SAME_COURSE);
        int credits = existing.stream().mapToInt(item -> Optional.ofNullable(item.getCredit()).orElse(0)).sum();
        if (credits + Optional.ofNullable(offering.getCredit()).orElse(0) > CREDIT_LIMIT)
            throw new MyException(MyErrorCode.MOCK_ENROLLMENT_CREDIT_LIMIT);
        validateNoConflict(existing, offering);
        enrollmentRepository.save(MockEnrollment.create(member, semester, offering));
    }

    @Transactional
    public void cancelEnrollment(Member member, Long offeringId) {
        Semester semester = openSemester();
        MockEnrollment item = enrollmentRepository.findByMemberIdAndSemesterIdAndCourseOfferingId(member.getId(), semester.getId(), offeringId)
                .orElseThrow(() -> new MyException(MyErrorCode.MOCK_ENROLLMENT_NOT_FOUND));
        enrollmentRepository.delete(item);
    }

    @Transactional
    public TimetableImportResponseDto importPrimaryTimetable(Member member) {
        Semester semester = openSemester();
        TimeTable timetable = timeTableRepository.findByMemberIdAndSemesterIdAndIsPrimaryTrue(member.getId(), semester.getId())
                .orElseThrow(() -> new MyException(MyErrorCode.PRIMARY_TIMETABLE_NOT_FOUND));
        List<Long> added = new ArrayList<>();
        List<TimetableImportResponseDto.SkippedItem> skipped = new ArrayList<>();
        timeTableItemRepository.findAllByTimeTableId(timetable.getId()).stream()
                .filter(item -> item.getType() == TimeTableItemType.COURSE)
                .forEach(item -> {
                    Long offeringId = item.getCourseOffering().getId();
                    try { addWatchlist(member, offeringId); added.add(offeringId); }
                    catch (MyException exception) { skipped.add(new TimetableImportResponseDto.SkippedItem(offeringId, exception.getErrorCode().name())); }
                });
        return new TimetableImportResponseDto(timetable.getId(), timetable.getTimeTableName(), added.size(), skipped.size(), added, skipped);
    }

    private Semester openSemester() {
        return semesterRepository.findFirstByStatusOrderByStartDateDesc(SemesterStatus.OPEN)
                .orElseThrow(() -> new MyException(MyErrorCode.MOCK_REGISTRATION_NOT_OPEN));
    }
    private CourseOffering offering(Semester semester, Long id) {
        CourseOffering offering = offeringRepository.findById(id).orElseThrow(() -> new MyException(MyErrorCode.COURSE_OFFERING_NOT_FOUND));
        if (!offering.getSemester().getId().equals(semester.getId())) throw new MyException(MyErrorCode.NO_MATCH_SEMESTER);
        return offering;
    }
    private boolean sameCourse(CourseOffering left, CourseOffering right) {
        String leftCode = left.getCourse().getCourseCode();
        String rightCode = right.getCourse().getCourseCode();
        return leftCode != null && leftCode.equals(rightCode);
    }
    private void validateNoConflict(List<CourseOffering> existing, CourseOffering candidate) {
        List<CourseMeeting> candidateMeetings = meetingRepository.findAllByCourseOfferingId(candidate.getId());
        for (CourseOffering item : existing) {
            for (CourseMeeting left : candidateMeetings) {
                for (CourseMeeting right : meetingRepository.findAllByCourseOfferingId(item.getId())) {
                    if (left.getDay() == right.getDay() && left.getStartTime() != null && left.getEndTime() != null
                            && right.getStartTime() != null && right.getEndTime() != null
                            && left.getStartTime().isBefore(right.getEndTime()) && left.getEndTime().isAfter(right.getStartTime()))
                        throw new MyException(MyErrorCode.MOCK_SCHEDULE_CONFLICT);
                }
            }
        }
    }
    private List<CourseOfferingResponseDto> toResponses(List<CourseOffering> offerings) {
        List<CourseOffering> sorted = offerings.stream().sorted(Comparator
                .comparingInt((CourseOffering item) -> gradeOrder(item.getHyNameRaw()))
                .thenComparing(item -> nullSafe(item.getIsuCode()))
                .thenComparing(item -> nullSafe(item.getSubjectNumber()))
                .thenComparing(CourseOffering::getId)).toList();
        Map<Long, List<CourseMeeting>> meetings = meetingRepository.findAllByCourseOfferingIdIn(sorted.stream().map(CourseOffering::getId).toList()).stream()
                .collect(Collectors.groupingBy(item -> item.getCourseOffering().getId()));
        return sorted.stream().map(item -> CourseOfferingResponseDto.from(item,
                meetingService.mergeContinuousMeetings(meetings.getOrDefault(item.getId(), List.of())), false)).toList();
    }
    private int gradeOrder(String value) { return "전학년".equals(value) ? 0 : "1".equals(value) ? 1 : "2".equals(value) ? 2 : "3".equals(value) ? 3 : "4".equals(value) ? 4 : 99; }
    private String nullSafe(String value) { return value == null ? "" : value; }
}
