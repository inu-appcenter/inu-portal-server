package kr.inuappcenterportal.inuportal.domain.customSchedule.service;

import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.CustomScheduleMeetingCommand;
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
    public CustomSchedule createCustomSchedule(
            String title,
            List<CustomScheduleMeetingCommand> meetingCommands
    ) {
        CustomSchedule customSchedule = customScheduleRepository.save(
                CustomSchedule.create(title)
        );

        List<CustomScheduleMeeting> meetings = meetingCommands.stream()
                .map(meetingCommand -> CustomScheduleMeeting.create(
                        customSchedule,
                        meetingCommand.location(),
                        meetingCommand.day(),
                        meetingCommand.startTime(),
                        meetingCommand.endTime()
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
            String title,
            List<CustomScheduleMeetingCommand> meetingCommands
    ) {
        customSchedule.setCustomScheduleTitle(title);

        customScheduleMeetingRepository.deleteAllByCustomScheduleId(customSchedule.getId());

        List<CustomScheduleMeeting> meetings = meetingCommands.stream()
                .map(meetingCommand -> CustomScheduleMeeting.create(
                        customSchedule,
                        meetingCommand.location(),
                        meetingCommand.day(),
                        meetingCommand.startTime(),
                        meetingCommand.endTime()
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
