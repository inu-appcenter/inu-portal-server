package kr.inuappcenterportal.inuportal.domain.customSchedule.service;

import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleRequestDto;
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
public class CustomScheduleService {

    private final CustomScheduleRepository customScheduleRepository;
    private final CustomScheduleMeetingRepository customScheduleMeetingRepository;


    /**
     * 커스텀일정 생성 메서드
     */
    @Transactional
    public CustomSchedule createCustomSchedule(CustomScheduleRequestDto request) {
        CustomSchedule customSchedule = customScheduleRepository.save(
                CustomSchedule.create(request.title())
        );

        List<CustomScheduleMeeting> meetings = request.meetings().stream()
                .map(meetingRequest -> CustomScheduleMeeting.create(
                        customSchedule,
                        meetingRequest.location(),
                        meetingRequest.day(),
                        meetingRequest.startTime(),
                        meetingRequest.endTime()
                ))
                .toList();

        customScheduleMeetingRepository.saveAll(meetings);

        return customSchedule;
    }


    /**
     * 커스텀일정 수정 메서드
     */
    @Transactional
    public void updateCustomSchedule(
            CustomSchedule customSchedule,
            CustomScheduleRequestDto request

    ) {
        customSchedule.setCustomScheduleTitle(request.title());

        customScheduleMeetingRepository.deleteAllByCustomScheduleId(customSchedule.getId());

        List<CustomScheduleMeeting> meetings = request.meetings().stream()
                .map(meetingRequest -> CustomScheduleMeeting.create(
                        customSchedule,
                        meetingRequest.location(),
                        meetingRequest.day(),
                        meetingRequest.startTime(),
                        meetingRequest.endTime()
                ))
                .toList();

        customScheduleMeetingRepository.saveAll(meetings);
    }


    /**
     * 커스텀일정 삭제 메서드
     */
    @Transactional
    public void deleteCustomSchedule(CustomSchedule customSchedule) {
        customScheduleMeetingRepository.deleteAllByCustomScheduleId(customSchedule.getId());
        customScheduleRepository.delete(customSchedule);
    }
}
