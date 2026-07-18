package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.service.CustomScheduleMeetingService;
import kr.inuappcenterportal.inuportal.domain.customSchedule.service.CustomScheduleService;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.TimeTableItemResponseDto;
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
    private final CustomScheduleMeetingService customScheduleMeetingService;
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
            CustomScheduleCreateRequestDto request) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("해당 시간표가 존재하지 않습니다."));

        validateOwner(memberId, timeTable);

        CustomSchedule customSchedule = customScheduleService.createCustomSchedule(request);

        TimeTableItem customScheduleItem = TimeTableItem.createForCustomSchedule(memo, timeTable, customSchedule);

        TimeTableItem saved = timeTableItemRepository.save(customScheduleItem);

        return TimeTableItemResponseDto.from(saved);
    }
}
