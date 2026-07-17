package kr.inuappcenterportal.inuportal.domain.customSchedule.service;

import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleTitleUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.response.CustomScheduleResponseDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
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
public class CustomScheduleService {

    private final CustomScheduleRepository customScheduleRepository;
    private final CustomScheduleMeetingRepository customScheduleMeetingRepository;
    private final MemberRepository memberRepository;
    private final SemesterRepository semesterRepository;
    private final CustomScheduleMeetingService customScheduleMeetingService;
    private final TimeTableItemRepository timeTableItemRepository;


    /**
     * 커스텀일정 생성 메서드
     */
    @Transactional
    public CustomScheduleResponseDto createCustomSchedule(
            Long memberId,
            Long semesterId,
            CustomScheduleCreateRequestDto request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저 존재하지 않습니다."));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("해당 학기가 존재하지 않습니다."));


        // 커스텀일정 생성 및 저장
        CustomSchedule customSchedule = customScheduleRepository.save(
                CustomSchedule.create(member, semester, request.title())
        );

        // 커스텀일정 시간 생성
        List<CustomScheduleMeeting> meetings =
                customScheduleMeetingService.createMeetings(customSchedule, request.meetings());

        return CustomScheduleResponseDto.from(customSchedule, meetings);
    }

    /**
     * 커스텀일정 소유권 검증 메서드
     */
    private void validateOwner(CustomSchedule customSchedule, Long memberId) {
        if (!customSchedule.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 커스텀일정에 접근할 권한이 없습니다.");
        }
    }


    /**
     * 커스텀일정 이름 수정 메서드
     */
    @Transactional
    public CustomScheduleResponseDto setCustomScheduleTitle(
            Long memberId,
            Long customScheduleId,
            CustomScheduleTitleUpdateRequestDto request
    ) {
        CustomSchedule customSchedule = customScheduleRepository.findById(customScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 커스텀일정이 존재하지 않습니다."));

        validateOwner(customSchedule, memberId);

        customSchedule.setCustomScheduleTitle(request.title());

        return CustomScheduleResponseDto.from(customSchedule);
    }


    /**
     * 커스텀일정 삭제 메서드
     */
    @Transactional
    public void deleteCustomSchedule(
            Long memberId,
            Long customScheduleId
    ) {
        CustomSchedule customSchedule = customScheduleRepository.findById(customScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 커스텀일정이 존재하지 않습니다."));

        validateOwner(customSchedule, memberId);

        timeTableItemRepository.deleteAllByCustomScheduleId(customScheduleId);
        customScheduleMeetingRepository.deleteAllByCustomScheduleId(customScheduleId);
        customScheduleRepository.delete(customSchedule);
    }
}
