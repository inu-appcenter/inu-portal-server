package kr.inuappcenterportal.inuportal.domain.customSchedule.service;

import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleMeetingRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.response.CustomScheduleMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
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
    private final TimeTableItemRepository timeTableItemRepository;

    /**
     * 커스텀일정 소유권 검증 메서드
     */
    private void validateOwner(CustomSchedule customSchedule, Long memberId) {
        if (!customSchedule.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 커스텀일정에 접근할 권한이 없습니다.");
        }
    }

    /**
     * 커스텀일정 시간 검증 메서드
     */
    private void validateMeetingBelongsToSchedule(CustomScheduleMeeting meeting, Long customScheduleId) {
        if (!meeting.getCustomSchedule().getId().equals(customScheduleId)) {
            throw new IllegalArgumentException("해당 커스텀일정에 속한 시간이 아닙니다");
        }
    }

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
    public CustomScheduleMeetingResponseDto updateMeetings(
            Long memberId,
            Long customScheduleId,
            Long meetingId,
            CustomScheduleMeetingRequestDto request
    ) {
        CustomSchedule customSchedule = customScheduleRepository.findById(customScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 커스텀일정이 존재하지 않습니다."));

        validateOwner(customSchedule, memberId);

        CustomScheduleMeeting meeting = customScheduleMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 커스텀일정 시간이 존재하지 않습니다."));

        validateMeetingBelongsToSchedule(meeting, customScheduleId);

        meeting.update(
                request.location(),
                request.day(),
                request.startTime(),
                request.endTime()
        );

        return CustomScheduleMeetingResponseDto.from(meeting);
    }


    /**
     * 커스텀일정 삭제 메서드
     */
    @Transactional
    public void deleteMeeting(
            Long memberId,
            Long customScheduleId,
            Long meetingId
    ) {
        CustomSchedule customSchedule = customScheduleRepository.findById(customScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 커스텀일정이 존재하지 않습니다."));

        validateOwner(customSchedule, memberId);

        CustomScheduleMeeting meeting = customScheduleMeetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 커스텀일정 시간이 존재하지 않습니다."));

        validateMeetingBelongsToSchedule(meeting, customScheduleId);

        long remainingMeetingCount =
                customScheduleMeetingRepository.countByCustomScheduleId(customScheduleId);

        customScheduleMeetingRepository.delete(meeting);

        if (remainingMeetingCount == 1) {
            timeTableItemRepository.deleteAllByCustomScheduleId(customScheduleId);
            customScheduleRepository.delete(customSchedule);
        }
    }
}
