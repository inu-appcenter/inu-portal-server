package kr.inuappcenterportal.inuportal.domain.customSchedule.service;

import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleMeetingRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CustomScheduleMeetingService {
    private final CustomScheduleMeetingRepository customScheduleMeetingRepository;
    private final CustomScheduleRepository customScheduleRepository;


    /**
     * 커스텀일정 시간 생성 메서드
     */
    @Transactional
    public List<CustomScheduleMeeting> createMeetings(
            CustomSchedule customSchedule,
            List<CustomScheduleMeetingRequestDto> requests

    ) {
        List<CustomScheduleMeeting> meetings = requests.stream()
                .map(request -> CustomScheduleMeeting.create(
                        customSchedule,
                        request.location(),
                        request.day(),
                        request.startTime(),
                        request.endTime()
                ))
                .toList();

        return customScheduleMeetingRepository.saveAll(meetings);
    }


    /**
     * 커스텀일정 시간 수정 메서드
     */
    @Transactional
    public CustomScheduleMeeting updateMeetings(
            Long meetingId,
            CustomScheduleMeetingRequestDto request
    ) {
        CustomScheduleMeeting meeting = customScheduleMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 커스텀일정 시간이 존재하지 않습니다."));

        meeting.update(
                request.location(),
                request.day(),
                request.startTime(),
                request.endTime()
        );

        return meeting;
    }


    /**
     * 커스텀일정 시간 삭제 메서드
     */
    @Transactional
    public void deleteMeeting(
            Long customScheduleId,
            Long meetingId
    ) {
        CustomScheduleMeeting meeting = customScheduleMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 커스텀일정 시간이 존재하지 않습니다."));

        validateMeetingBelongsToSchedule(meeting, customScheduleId);

        customScheduleMeetingRepository.delete(meeting);
    }

    private void validateMeetingBelongsToSchedule(
            CustomScheduleMeeting meeting,
            Long customScheduleId
    ) {
        if (!meeting.getCustomSchedule().getId().equals(customScheduleId)) {
            throw new IllegalArgumentException("해당 커스텀일정에 속한 시간이 아닙니다.");
        }
    }
}
