package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import kr.inuappcenterportal.inuportal.domain.course.enums.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.CustomScheduleMeetingCommand;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.service.CustomScheduleService;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem.TimeTableCustomItemRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.TimeTableItemType;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TimeTableItemService {

    private final TimeTableItemRepository timeTableItemRepository;
    private final TimeTableRepository timeTableRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CustomScheduleService customScheduleService;
    private final CourseMeetingRepository courseMeetingRepository;


    /**
     * 강의 기반 시간표 요소 생성 메서드
     */
    @Transactional
    public TimeTableItemResponseDto createTimeTableItemForCourse(
            String memo,
            Long memberId,
            Long timeTableId,
            Long courseOfferingId
    ) {
        // 시간표 가져오기
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_NOT_FOUND));

        // 시간표 유저 검증
        validateOwner(memberId, timeTable);

        // 개설 강의 가져오기
        CourseOffering courseOffering = courseOfferingRepository.findById(courseOfferingId)
                .orElseThrow(() -> new MyException(MyErrorCode.COURSE_OFFERING_NOT_FOUND));

        // 시간표의 학기와 개설 겅의 학기가 맞는지 검증
        if (!courseOffering.getSemester().getId().equals(timeTable.getSemester().getId())) {
            throw new MyException(MyErrorCode.NO_MATCH_SEMESTER);
        }

        // 동일한 개설 강의 요소가 들어가는지 검증
        if (timeTableItemRepository.existsByTimeTableIdAndCourseOfferingId(timeTableId, courseOfferingId)) {
            throw new MyException(MyErrorCode.DUPLICATE_TIMETABLE_COURSE_ITEM);
        }

        // 생성용
        List<CourseMeeting> meetings = courseMeetingRepository.findAllByCourseOfferingId(courseOffering.getId());
        if (!meetings.isEmpty()) {
            List<TimeSlot> timeSlots = toTimeSlots(meetings);

            validateNoRequestTimeConflict(timeSlots);
            validateNoDBTimeConflict(timeTableId, timeSlots);
        }

        TimeTableItem courseItem = TimeTableItem.createForCourse(memo, timeTable, courseOffering);

        TimeTableItem saved = timeTableItemRepository.save(courseItem);

        return TimeTableItemResponseDto.from(saved);
    }


    /**
     * 커스텀일정 기반 시간표 요소 생성 메서드
     */
    @Transactional
    public TimeTableItemResponseDto createTimeTableItemForCustom(
            Long memberId,
            Long timeTableId,
            TimeTableCustomItemRequestDto request
    ) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_NOT_FOUND));

        // 본인 시간표인지 검증
        validateOwner(memberId, timeTable);

        // 생성용
        List<CustomScheduleMeetingCommand> meetings = toCustomCommands(request);
        // 검증용
        List<TimeSlot> timeSlots = toTimeSlots(request);

        // 중복 일정 검증
        validateNoRequestTimeConflict(timeSlots);
        validateNoDBTimeConflict(timeTableId, timeSlots);

        // 커스텀일정 생성
        CustomSchedule customSchedule = customScheduleService.createCustomSchedule(request.title(), meetings);

        // 커스텀일정 기반 시간표 요소 생성
        TimeTableItem customScheduleItem = TimeTableItem.createForCustomSchedule(request.memo(), timeTable, customSchedule);

        // 위에서 만든 시간표 요소를 DB에 저장
        TimeTableItem saved = timeTableItemRepository.save(customScheduleItem);

        return TimeTableItemResponseDto.from(saved);
    }

    /**
     * 커스텀일정 기반 시간표 요소 수정 메서드
     */
    @Transactional
    public TimeTableItemResponseDto updateTimeTableItemForCustom(
            Long memberId,
            Long timeTableId,
            Long customScheduleId,
            TimeTableCustomItemRequestDto request
    ) {
        // 시간표 가져오기
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_NOT_FOUND));

        // 본인 시간표인지 검증
        validateOwner(memberId, timeTable);

        // 수정할 시간표 요소 가져오기
        TimeTableItem updatetimeTableItem = timeTableItemRepository.findByCustomScheduleId(customScheduleId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_ITEM_NOT_FOUND));

        // 위에서 찾은 시간표 요소가 해당 시간표에 속하는지 검증
        if (!updatetimeTableItem.getTimeTable().getId().equals(timeTableId)) {
            throw new MyException(MyErrorCode.NO_CUSTOM_ITEM_IN_TIMETABLE);
        }

        // 수정하려는 시간표 요소가 커스텀인지 아닌지 검증
        if (updatetimeTableItem.getType() != TimeTableItemType.CUSTOM)
            throw new MyException(MyErrorCode.NO_CUSTOM_ITEM);

        // 가져온 시간표 요소에서 커스텀 일정 정보 추출
        CustomSchedule customSchedule = updatetimeTableItem.getCustomSchedule();

        // 생성용
        List<CustomScheduleMeetingCommand> meetings = toCustomCommands(request);
        // 검증용
        List<TimeSlot> timeSlots = toTimeSlots(request);

        // 중복 일정 검증
        validateNoRequestTimeConflict(timeSlots);
        validateNoDBTimeConflict(timeTableId, timeSlots, updatetimeTableItem.getId());

        customScheduleService.updateCustomSchedule(
                customSchedule,
                request.title(),
                meetings
        );

        return TimeTableItemResponseDto.from(updatetimeTableItem);
    }

    // 중복 변환 로직 분리
    private List<CustomScheduleMeetingCommand> toCustomCommands(TimeTableCustomItemRequestDto request) {
        return request.meetings().stream()
                .map(meeting -> new CustomScheduleMeetingCommand(
                        meeting.location(),
                        meeting.day(),
                        meeting.startTime(),
                        meeting.endTime()
                ))
                .toList();
    }

    // 커스텀 일정용
    private List<TimeSlot> toTimeSlots(TimeTableCustomItemRequestDto request) {
        return request.meetings().stream()
                .map(meeting -> new TimeSlot(
                        meeting.day(),
                        meeting.startTime(),
                        meeting.endTime()
                ))
                .toList();
    }

    // 개설 강의용
    private List<TimeSlot> toTimeSlots(List<CourseMeeting> meetings) {
        return meetings.stream()
                .map(meeting -> new TimeSlot(
                        meeting.getDay(),
                        meeting.getStartTime(),
                        meeting.getEndTime()
                ))
                .toList();
    }


    /**
     * 시간표 요소 단일 삭제 메서드
     */
    @Transactional
    public void deleteTimeTableItem(
            Long memberId,
            Long timeTableId,
            Long timeTableItemId
    ) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_NOT_FOUND));

        // 본인 시간표인지 검증
        validateOwner(memberId, timeTable);

        TimeTableItem timeTableItem = timeTableItemRepository.findById(timeTableItemId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_ITEM_NOT_FOUND));

        validateItemBelongsToTimeTable(timeTableItem, timeTableId);

        deleteTimeTableItemWithReferences(timeTableItem);
    }

    /**
     * 시간표 요소 전체 삭제 메서드
     * (시간표 삭제에 들어감, 컨트롤러에서 호출되는 메서드 아님)
     */
    @Transactional
    public void deleteAllTimeTableItems(TimeTable timeTable) {
        List<TimeTableItem> items = timeTableItemRepository.findAllByTimeTableId(timeTable.getId());

        items.forEach(this::deleteTimeTableItemWithReferences);
    }

    /**
     * 시간표 요소에 참조된 데이터 삭제 메서드
     */
    private void deleteTimeTableItemWithReferences(TimeTableItem timeTableItem) {
        boolean isCustomItem = timeTableItem.getType() == TimeTableItemType.CUSTOM;
        CustomSchedule customSchedule = timeTableItem.getCustomSchedule();

        timeTableItemRepository.delete(timeTableItem);

        if (isCustomItem) {
            customScheduleService.deleteCustomSchedule(customSchedule);
        }
    }

    /**
     * 시간표 요소가 해당 시간표에 속하는지 검증하는 메서드
     */
    private void validateItemBelongsToTimeTable(TimeTableItem timeTableItem, Long timeTableId) {
        if (!timeTableItem.getTimeTable().getId().equals(timeTableId)) {
            throw new MyException(MyErrorCode.NO_ITEM_IN_TIMETABLE);
        }
    }

    /**
     * 시간표 소유권 검증 메서드
     */
    private void validateOwner(Long memberId, TimeTable timeTable) {
        if (!timeTable.getMember().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.HAS_NOT_TIMETABLE_AUTHORIZATION);
        }
    }

    /**
     * 시간표 요소 시간 및 요일 중복 차단 메서드(DB, 수정용)
     */
    private void validateNoDBTimeConflict(
            Long timeTableId,
            List<TimeSlot> meetings,
            Long excludeTimeTableItemId // 수정할 때 자기 자신을 충돌 검사 대상에서 제외하기 위한 ID
    ) {
        for (TimeSlot meeting : meetings) {

            boolean exists = timeTableItemRepository.existsOverlappingMeeting(
                    timeTableId,
                    meeting.day(),
                    meeting.startTime(),
                    meeting.endTime(),
                    excludeTimeTableItemId
            );

            if (exists) {
                throw new MyException(MyErrorCode.TIMETABLE_ITEM_TIME_DB_CONFLICT);
            }
        }
    }

    /**
     * 시간표 요소 시간 및 요일 중복 차단 메서드(DB, 생성용)
     */
    private void validateNoDBTimeConflict(
            Long timeTableId,
            List<TimeSlot> meetings
    ) {
        for (TimeSlot meeting : meetings) {

            boolean exists = timeTableItemRepository.existsOverlappingMeeting(
                    timeTableId,
                    meeting.day(),
                    meeting.startTime(),
                    meeting.endTime()
            );

            if (exists) {
                throw new MyException(MyErrorCode.TIMETABLE_ITEM_TIME_DB_CONFLICT);
            }
        }
    }

    /**
     * 시간표 요소 시간 및 요일 중복 차단 메서드(요청)
     */
    private void validateNoRequestTimeConflict(List<TimeSlot> meetings) {
        meetings.forEach(this::validateTimeSlot);

        for (int i = 0; i < meetings.size(); i++) {
            TimeSlot current = meetings.get(i);

            for (int j = i + 1; j < meetings.size(); j++) {
                TimeSlot other = meetings.get(j);

                if (current.day() == other.day()
                        && current.startTime().isBefore(other.endTime())
                        && current.endTime().isAfter(other.startTime())) {
                    throw new MyException(MyErrorCode.TIMETABLE_ITEM_TIME_REQUEST_CONFLICT);
                }
            }
        }
    }

    private void validateTimeSlot(TimeSlot meeting) {
        if (meeting.day() == null) {
            throw new MyException(MyErrorCode.NECESSARY_DAY_OF_WEEK);
        }

        if (meeting.startTime() == null || meeting.endTime() == null) {
            throw new MyException(MyErrorCode.NECESSARY_STARTTIME_AND_ENDTIME);
        }

        if (!meeting.startTime().isBefore(meeting.endTime())) {
            throw new MyException(MyErrorCode.FASTER_THAN_ENDTIME);
        }
    }

    /**
     * 시간 검증용 내부 객체
     */
    private record TimeSlot(
            DayOfWeek day,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
