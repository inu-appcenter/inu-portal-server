package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.CustomScheduleMeetingCommand;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.service.CustomScheduleService;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem.TimeTableCustomItemRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.TimeTableItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.enums.TimeTableItemType;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    /**
     * 강의 기반 시간표 요소 생성 메서드
     */
    @Transactional
    public TimeTableItemResponseDto createTimeTableItemForCourse(
            String memo,
            Long memberId,
            Long timeTableId,
            Long courseOfferingId) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("해당 시간표가 존재하지 않습니다."));

        CourseOffering courseOffering = courseOfferingRepository.findById(courseOfferingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 강의가 존재하지 않습니다."));

        if (!courseOffering.getSemester().getId().equals(timeTable.getSemester().getId())) {
            throw new IllegalArgumentException("시간표의 학기와 강의의 학기가 일치하지 않습니다.");
        }

        validateOwner(memberId, timeTable);

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
            TimeTableCustomItemRequestDto request) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("해당 시간표가 존재하지 않습니다."));

        // 본인 시간표인지 검증v
        validateOwner(memberId, timeTable);

        List<CustomScheduleMeetingCommand> meetings = request.meetings().stream()
                .map(meeting -> new CustomScheduleMeetingCommand(
                        meeting.location(),
                        meeting.day(),
                        meeting.startTime(),
                        meeting.endTime()
                ))
                .toList();

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
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("해당 시간표가 존재하지 않습니다."));

        // 본인 시간표인지 검증
        validateOwner(memberId, timeTable);

        TimeTableItem timeTableItem = timeTableItemRepository.findByCustomScheduleId(customScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 시간표 요소가 존재하지 않습니다."));

        // 위에서 찾은 시간표 요소가 해당 시간표에 속하는지 검증
        if (!timeTableItem.getTimeTable().getId().equals(timeTableId)) {
            throw new IllegalArgumentException("해당 시간표에 속한 커스텀일정이 아닙니다.");
        }

        if (timeTableItem.getType() != TimeTableItemType.CUSTOM)
            throw new IllegalArgumentException("해당 요소는 커스텀 일정이 아닙니다.");

        CustomSchedule customSchedule = timeTableItem.getCustomSchedule();

        List<CustomScheduleMeetingCommand> meetings = request.meetings().stream()
                .map(meeting -> new CustomScheduleMeetingCommand(
                        meeting.location(),
                        meeting.day(),
                        meeting.startTime(),
                        meeting.endTime()
                ))
                .toList();

        customScheduleService.updateCustomSchedule(
                customSchedule,
                request.title(),
                meetings
        );

        return TimeTableItemResponseDto.from(timeTableItem);
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
                .orElseThrow(() -> new IllegalArgumentException("해당 시간표가 존재하지 않습니다."));

        // 본인 시간표인지 검증
        validateOwner(memberId, timeTable);

        TimeTableItem timeTableItem = timeTableItemRepository.findById(timeTableItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 시간표 요소가 존재하지 않습니다."));

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
     * 시간표 요소 검증 메서드
     */
    private void validateItemBelongsToTimeTable(TimeTableItem timeTableItem, Long timeTableId) {
        if (!timeTableItem.getTimeTable().getId().equals(timeTableId)) {
            throw new IllegalArgumentException("해당 시간표에 속한 요소가 아닙니다.");
        }
    }

    /**
     * 시간표 소유권 검증 메서드
     */
    private void validateOwner(Long memberId, TimeTable timeTable) {
        if (!timeTable.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 시간표에 접근할 권한이 없습니다.");
        }
    }
}
