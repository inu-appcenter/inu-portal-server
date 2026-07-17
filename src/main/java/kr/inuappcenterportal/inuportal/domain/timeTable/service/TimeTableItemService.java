package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
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

    @Transactional
    public TimeTableItemResponseDto createTimeTableItemForCourse(String memo, Long timeTableId, Long courseOfferingId) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("해당 시간표가 존재하지 않습니다."));

        CourseOffering courseOffering = courseOfferingRepository.findById(courseOfferingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 강의가 존재하지 않습니다."));

        TimeTableItem courseItem = TimeTableItem.createForCourse(memo, timeTable, courseOffering);

        TimeTableItem saved = timeTableItemRepository.save(courseItem);

        return TimeTableItemResponseDto.from(saved);
    }

    @Transactional
    public TimeTableItemResponseDto createTimeTableItemForCustom(String memo, Long timeTableId, Long customScheduleId) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("해당 시간표가 존재하지 않습니다."));

        return null;
    }
}
