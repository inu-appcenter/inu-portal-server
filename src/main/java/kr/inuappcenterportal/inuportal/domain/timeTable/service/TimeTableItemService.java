package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.service.CustomScheduleService;
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
     * 시간표 소유권 검증 메서드
     */
    private void validateOwner(Long memberId, TimeTable timeTable) {
        if (!timeTable.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 시간표에 접근할 권한이 없습니다.");
        }
    }

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
            String memo,
            Long memberId,
            Long timeTableId,
            CustomScheduleRequestDto request) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("해당 시간표가 존재하지 않습니다."));

        // 본인 시간표인지 검증
        validateOwner(memberId, timeTable);

        // 커스텀일정 생성
        CustomSchedule customSchedule = customScheduleService.createCustomSchedule(request);

        // 커스텀일정 기반 시간표 요소 생성
        TimeTableItem customScheduleItem = TimeTableItem.createForCustomSchedule(memo, timeTable, customSchedule);

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
            CustomScheduleRequestDto request
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

        CustomSchedule customSchedule = timeTableItem.getCustomSchedule();

        customScheduleService.updateCustomSchedule(customSchedule, request);

        return TimeTableItemResponseDto.from(timeTableItem);
    }

    /**
     * 시간표 요소 삭제 메서드
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

        if (!timeTableItem.getTimeTable().getId().equals(timeTableId)) {
            throw new IllegalArgumentException("해당 시간표에 속한 요소가 아닙니다.");
        }

        boolean isCustomItem = timeTableItem.getType() == TimeTableItemType.CUSTOM;
        CustomSchedule customSchedule = timeTableItem.getCustomSchedule();

        timeTableItemRepository.delete(timeTableItem);

        if (isCustomItem) {
            customScheduleService.deleteCustomSchedule(customSchedule);
        }
    }
}
