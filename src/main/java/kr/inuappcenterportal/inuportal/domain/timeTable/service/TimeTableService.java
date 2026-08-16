package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseMeetingService;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.FriendService;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable.TimeTableCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable.TimeTableNameUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable.TimeTableVisibilityUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.CourseTimeTableItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.CustomTimeTableItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableDetailItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.Visibility;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableEvaluationRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TimeTableService {

    private final TimeTableRepository timeTableRepository;
    private final TimeTableItemRepository timeTableItemRepository;
    private final MemberRepository memberRepository;
    private final SemesterRepository semesterRepository;
    private final TimeTableItemService timeTableItemService;
    private final CourseMeetingRepository courseMeetingRepository;
    private final CustomScheduleMeetingRepository customScheduleMeetingRepository;
    private final FriendService friendService;
    private final CourseMeetingService courseMeetingService;
    private final TimeTableEvaluationRepository timeTableEvaluationRepository;


    /**
     * 내 시간표 상세 조회 메서드
     */
    public TimeTableDetailResponseDto getTimeTableDetail(
            Long memberId,
            Long timeTableId
    ) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_NOT_FOUND));

        validateOwner(timeTable, memberId);

        List<TimeTableDetailItemResponseDto> items = timeTableItemRepository.findAllByTimeTableId(timeTableId).stream()
                .map(this::toDetailItemResponse)
                .toList();

        return TimeTableDetailResponseDto.from(timeTable, items);
    }


    // 본인용
    private TimeTableDetailItemResponseDto toDetailItemResponse(
            TimeTableItem item
    ) {
        return switch (item.getType()) {
            case COURSE -> {
                CourseOffering courseOffering = item.getCourseOffering();

                List<CourseMeeting> meetings =
                        courseMeetingRepository.findAllByCourseOfferingId(courseOffering.getId());

                List<CourseMeetingResponseDto> mergedMeetings =
                        courseMeetingService.mergeContinuousMeetings(meetings);

                yield TimeTableDetailItemResponseDto.ofCourse(
                        item,
                        CourseTimeTableItemResponseDto.of(courseOffering, mergedMeetings)
                );
            }

            case CUSTOM -> {
                CustomSchedule customSchedule = item.getCustomSchedule();

                List<CustomScheduleMeeting> meetings =
                        customScheduleMeetingRepository.findAllByCustomScheduleId(customSchedule.getId());

                yield TimeTableDetailItemResponseDto.ofCustom(
                        item,
                        CustomTimeTableItemResponseDto.of(customSchedule, meetings)
                );
            }
        };
    }


    /**
     * 시간표 생성 메서드
     */
    @Transactional
    public TimeTableResponseDto createTimeTable(
            Long memberId,
            Long semesterId,
            TimeTableCreateRequestDto createRequestDto
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        checkDuplicateTimeTableNameForCreate(memberId, semesterId, createRequestDto.timeTableName());

        boolean isFirstTimeTable = !timeTableRepository.existsByMemberIdAndSemesterId(memberId, semesterId);

        TimeTable timeTable = TimeTable.create(
                createRequestDto.timeTableName(),
                isFirstTimeTable,
                member,
                semester
        );

        TimeTable saved = timeTableRepository.save(timeTable);

        return TimeTableResponseDto.from(saved);
    }

    /**
     * 시간표 이름 중복 확인 메서드(생성용)
     */
    private void checkDuplicateTimeTableNameForCreate(
            Long memberId,
            Long semesterId,
            String checkName
    ) {
        if (timeTableRepository.existsByMemberIdAndSemesterIdAndTimeTableName(
                memberId,
                semesterId,
                checkName
        )) {
            throw new MyException(MyErrorCode.DUPLICATE_TIMETABLE_NAME);
        }
    }

    /**
     * 시간표 이름 중복 확인 메서드(수정용)
     */
    private void checkDuplicateTimeTableNameForUpdate(
            Long memberId,
            Long semesterId,
            String checkName,
            Long timeTableId
    ) {
        if (timeTableRepository.existsByMemberIdAndSemesterIdAndTimeTableNameAndIdNot(
                memberId,
                semesterId,
                checkName,
                timeTableId
        )) {
            throw new MyException(MyErrorCode.DUPLICATE_TIMETABLE_NAME);
        }
    }


    /**
     * 시간표 공개범위 수정 메서드
     */
    @Transactional
    public TimeTableResponseDto setVisibility(
            Long memberId,
            Long timeTableId,
            TimeTableVisibilityUpdateRequestDto visibilityUpdateRequestDto) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_NOT_FOUND));

        validateOwner(timeTable, memberId);

        timeTable.updateVisibility(visibilityUpdateRequestDto.visibility());
        return TimeTableResponseDto.from(timeTable);
    }


    /**
     * 대표 시간표 수정 메서드
     */
    @Transactional
    public TimeTableResponseDto setIsPrimary(Long memberId, Long timeTableId) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_NOT_FOUND));

        validateOwner(timeTable, memberId);

        if (timeTable.isPrimary()) {
            return TimeTableResponseDto.from(timeTable);
        }

        timeTableRepository.findByMemberIdAndSemesterIdAndIsPrimaryTrue(
                timeTable.getMember().getId(),
                timeTable.getSemester().getId()
        ).ifPresent(TimeTable::unmarkPrimary);

        timeTable.markPrimary();
        return TimeTableResponseDto.from(timeTable);
    }

    /**
     * 시간표 이름 변경 메서드
     */
    private void AutoUpdatePrimary(TimeTable deletingTimeTable) {
        if (!deletingTimeTable.isPrimary()) {
            return;
        }

        timeTableRepository.findAllByMemberIdAndSemesterId(deletingTimeTable.getMember().getId(), deletingTimeTable.getSemester().getId())
                .stream()
                .filter(timeTable -> !timeTable.getId().equals(deletingTimeTable.getId()))
                .findFirst()
                .ifPresent(TimeTable::markPrimary);
    }

    /**
     * 시간표 이름 변경 메서드
     */
    @Transactional
    public TimeTableResponseDto setTimeTableName(
            Long memberId,
            Long timeTableId,
            TimeTableNameUpdateRequestDto nameUpdateRequestDto) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_NOT_FOUND));

        validateOwner(timeTable, memberId);

        checkDuplicateTimeTableNameForUpdate(
                timeTable.getMember().getId(),
                timeTable.getSemester().getId(),
                nameUpdateRequestDto.timeTableName(),
                timeTableId
        );

        timeTable.updateTimeTableName(nameUpdateRequestDto.timeTableName());

        return TimeTableResponseDto.from(timeTable);
    }

    /**
     * 시간표 삭제 메서드
     */
    @Transactional
    public void deleteTimeTable(Long memberId, Long timeTableId) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_NOT_FOUND));

        validateOwner(timeTable, memberId);

        AutoUpdatePrimary(timeTable);

        timeTableItemService.deleteAllTimeTableItems(timeTable);

        timeTableEvaluationRepository.deleteByTimeTableId(timeTableId);

        timeTableRepository.delete(timeTable);
    }

    /**
     * 사용자 검증 메서드
     */
    private void validateOwner(TimeTable timeTable, Long memberId) {
        if (!timeTable.getMember().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.HAS_NOT_TIMETABLE_AUTHORIZATION);
        }
    }


    /**
     * 친구의 시간표를 읽을 수 있는지 검증하는 메서드
     */
    private Visibility validateFriendTimeTableVisibility(TimeTable timeTable) {
        return switch (timeTable.getVisibility()) {
            case PRIVATE -> throw new MyException(MyErrorCode.PRIVATE_TIMETABLE);
            case PROTECTED -> Visibility.PROTECTED;
            case PUBLIC -> Visibility.PUBLIC;
        };
    }


    /**
     * 시간표 전체 조회
     */
    public List<TimeTableResponseDto> getTimeTables(Long memberId) {
        return timeTableRepository.findAllByMemberId(memberId).stream()
                .map(TimeTableResponseDto::from)
                .toList();
    }

    /**
     * 학기별 시간표 조회(id)
     */
    public List<TimeTableResponseDto> getTimeTablesOfSemester(Long memberId, Long semesterId) {
        if (!semesterRepository.existsById(semesterId))
            throw new MyException(MyErrorCode.SEMESTER_NOT_FOUND);

        return timeTableRepository.findAllByMemberIdAndSemesterId(memberId, semesterId).stream()
                .map(TimeTableResponseDto::from)
                .toList();
    }

    /**
     * 학기별 시간표 조회 (년도+학기)
     */
    public List<TimeTableResponseDto> getTimeTablesOfYearAndTerm(Long memberId, Integer year, SemesterTerm term) {
        if (year == null || term == null) {
            throw new MyException(MyErrorCode.INPUT_YEAR_AND_TERM);
        }

        Long semesterId = semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND))
                .getId();

        return timeTableRepository.findAllByMemberIdAndSemesterId(memberId, semesterId).stream()
                .map(TimeTableResponseDto::from)
                .toList();
    }


    /**
     * 학기별 친구 대표 시간표 상세 조회 (년도+학기)
     */
    public TimeTableDetailResponseDto getFriendPrimaryTimeTableDetailYearAndTerm(
            Long viewerId,
            Long friendMemberId,
            Integer year,
            SemesterTerm term
    ) {
        // 입력 검증
        if (year == null || term == null) {
            throw new MyException(MyErrorCode.INPUT_YEAR_AND_TERM);
        }

        // 학기 가져오기
        Long semesterId = semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND))
                .getId();

        // 친구관계인지 검증
        if (!friendService.isReadableFriend(viewerId, friendMemberId)) {
            throw new MyException(MyErrorCode.NOT_READABLE_TIMETABLE);
        }

        // 친구의 시간표 가져오기
        TimeTable timeTable = timeTableRepository
                .findByMemberIdAndSemesterIdAndIsPrimaryTrue(friendMemberId, semesterId)
                .orElseThrow(() -> new MyException(MyErrorCode.PRIMARY_TIMETABLE_NOT_FOUND));

        // 친구 시간표의 공개범위 확인
        Visibility visibility = validateFriendTimeTableVisibility(timeTable);

        // 공개범위에 따라 가져올 item 선택
        List<TimeTableDetailItemResponseDto> items =
                timeTableItemRepository.findAllByTimeTableId(timeTable.getId()).stream()
                        .map(item -> toDetailItemResponse(item, visibility))
                        .toList();

        return TimeTableDetailResponseDto.from(timeTable, items);
    }

    // 친구용
    private TimeTableDetailItemResponseDto toDetailItemResponse(
            TimeTableItem item,
            Visibility visibility
    ) {
        return switch (item.getType()) {
            case COURSE -> {
                CourseOffering courseOffering = item.getCourseOffering();

                List<CourseMeeting> meetings =
                        courseMeetingRepository.findAllByCourseOfferingId(courseOffering.getId());

                List<CourseMeetingResponseDto> mergedMeetings =
                        courseMeetingService.mergeContinuousMeetings(meetings);

                yield TimeTableDetailItemResponseDto.ofCourse(
                        item,
                        CourseTimeTableItemResponseDto.of(courseOffering, mergedMeetings, visibility),
                        visibility
                );
            }

            case CUSTOM -> {
                CustomSchedule customSchedule = item.getCustomSchedule();

                List<CustomScheduleMeeting> meetings =
                        customScheduleMeetingRepository.findAllByCustomScheduleId(customSchedule.getId());

                yield TimeTableDetailItemResponseDto.ofCustom(
                        item,
                        CustomTimeTableItemResponseDto.of(customSchedule, meetings, visibility),
                        visibility
                );
            }
        };
    }
}
