package kr.inuappcenterportal.inuportal.domain.customSchedule.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.customSchedule.dto.request.CustomScheduleCreateRequestDto;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
