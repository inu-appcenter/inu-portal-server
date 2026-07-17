package kr.inuappcenterportal.inuportal.domain.customSchedule.service;

import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.response.CustomScheduleResponseDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomSchedule;
import kr.inuappcenterportal.inuportal.domain.customSchedule.model.CustomScheduleMeeting;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.customSchedule.repository.CustomScheduleRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
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


    /**
     * 커스텀일정 생성 메서드
     */
    @Transactional
    public CustomScheduleResponseDto createCustomSchedule(
            Long memberId,
            Long semesterId,
            CustomScheduleRequestDto request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저 존재하지 않습니다."));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("해당 학기가 존재하지 않습니다."));


        // 커스텀일정 생성 및 저장
        CustomSchedule customSchedule = customScheduleRepository.save(
                CustomSchedule.create(member, semester, request.title())
        );

        // 커스텀일정 시간 생성
        List<CustomScheduleMeeting> meetings = request.meetings().stream()
                .map(meeting -> CustomScheduleMeeting.create(
                        customSchedule,
                        meeting.location(),
                        meeting.day(),
                        meeting.startTime(),
                        meeting.endTime()
                ))
                .toList();

        // 커스텀일정 시간 저장
        customScheduleMeetingRepository.saveAll(meetings);

        return CustomScheduleResponseDto.from(customSchedule, meetings);
    }
}
