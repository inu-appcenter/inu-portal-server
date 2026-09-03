package kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableImage;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;

import java.util.List;

public record TimeTableImageRecognizeResponseDto(
        String title,
        String professor,
        String classroom,
        String subjectNumber,
        List<TimeTableImageMeetingDto> meetings,
        List<CourseOfferingResponseDto> candidates,
        Long recommendedOfferingId
) {
}
