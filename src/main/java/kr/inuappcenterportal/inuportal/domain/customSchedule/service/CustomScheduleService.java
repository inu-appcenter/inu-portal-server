package kr.inuappcenterportal.inuportal.domain.customSchedule.service;

import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleTitleUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CustomScheduleService {

    private final CustomScheduleRepository customScheduleRepository;
    private final CustomScheduleMeetingRepository customScheduleMeetingRepository;
    private final CustomScheduleMeetingService customScheduleMeetingService;


    /**
     * 커스텀일정 생성 메서드
     */
    @Transactional
    public CustomSchedule createCustomSchedule(CustomScheduleCreateRequestDto request) {
        CustomSchedule customSchedule = customScheduleRepository.save(
                CustomSchedule.create(request.title())
        );

        customScheduleMeetingService.createMeetings(
                customSchedule,
                request.meetings()
        );

        return customSchedule;

    }


    /**
     * 커스텀일정 이름 수정 메서드
     */
    @Transactional
    public CustomSchedule updateTitle(
            CustomSchedule customSchedule,
            CustomScheduleTitleUpdateRequestDto request
    ) {
        customSchedule.setCustomScheduleTitle(request.title());
        
        return customSchedule;
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
