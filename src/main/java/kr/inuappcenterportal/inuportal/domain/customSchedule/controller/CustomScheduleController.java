package kr.inuappcenterportal.inuportal.domain.customSchedule.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleMeetingRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleTitleUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.response.CustomScheduleMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.response.CustomScheduleResponseDto;
import kr.inuappcenterportal.inuportal.domain.customSchedule.service.CustomScheduleMeetingService;
import kr.inuappcenterportal.inuportal.domain.customSchedule.service.CustomScheduleService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.dto.SemesterResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customSchedule")
public class CustomScheduleController {

    private final CustomScheduleService customScheduleService;
    private final CustomScheduleMeetingService customScheduleMeetingService;

    @PostMapping
    public ResponseEntity<ResponseDto<CustomScheduleResponseDto>> createCustomSchedule(
            @AuthenticationPrincipal Member member,
            @Valid SemesterResponseDto semester,
            @Valid CustomScheduleCreateRequestDto request
    ) {
        CustomScheduleResponseDto reponse =
                customScheduleService.createCustomSchedule(member.getId(), semester.id(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseDto.of(reponse, "커스텀일정 생성 성공")
        );
    }


    @PatchMapping
    public ResponseEntity<ResponseDto<CustomScheduleMeetingResponseDto>> updateCustomScheduleMeetings(
            @AuthenticationPrincipal Member member,
            @PathVariable Long customScheduleId,
            @PathVariable Long meetingId,
            @Valid CustomScheduleMeetingRequestDto request
    ) {
        CustomScheduleMeetingResponseDto reponse =
                customScheduleMeetingService.updateMeetings(member.getId(), customScheduleId, meetingId, request);

        return ResponseEntity.ok(
                ResponseDto.of(reponse, "커스텀일정 시간 수정 성공")
        );
    }

    @PatchMapping("/")
    public ResponseEntity<ResponseDto<CustomScheduleResponseDto>> updateCustomScheduleTitle(
            @AuthenticationPrincipal Member member,
            @PathVariable Long customScheduleId,
            @Valid CustomScheduleTitleUpdateRequestDto request
    ) {
        CustomScheduleResponseDto reponse =
                customScheduleService.setCustomScheduleTitle(member.getId(), customScheduleId, request);

        return ResponseEntity.ok(
                ResponseDto.of(reponse, "커스텀일정 제목 수정 성공")
        );
    }
}
