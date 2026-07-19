package kr.inuappcenterportal.inuportal.domain.timeTable.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem.TimeTableCourseItemRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem.TimeTableCustomItemRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableItemResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableItemService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
public class TimeTableItemController {

    private final TimeTableItemService timeTableItemService;

    /**
     * 강의 시간표 요소 생성 컨트롤러
     */
    @PostMapping("/{timeTableId}")
    public ResponseEntity<ResponseDto<TimeTableItemResponseDto>> createTimeTableItemForCourse(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId,
            @Valid @RequestBody TimeTableCourseItemRequestDto request
    ) {

        TimeTableItemResponseDto response =
                timeTableItemService.createTimeTableItemForCourse(
                        request.memo(), member.getId(), timeTableId, request.courseOfferingId()
                );

        return ResponseEntity.ok(
                ResponseDto.of(response, "강의 시간표 요소 생성 성공")
        );
    }


    /**
     * 커스텀일정 시간표 요소 생성 컨트롤러
     */
    @PostMapping("/{timeTableId}/customSchedule")
    public ResponseEntity<ResponseDto<TimeTableItemResponseDto>> createTimeTableItemForCustom(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId,
            @Valid @RequestBody TimeTableCustomItemRequestDto request
    ) {

        TimeTableItemResponseDto response =
                timeTableItemService.createTimeTableItemForCustom(
                        member.getId(), timeTableId, request
                );

        return ResponseEntity.ok(
                ResponseDto.of(response, "커스텀 일정 시간표 요소 생성 성공")
        );
    }


    /**
     * 커스텀일정 시간표 요소 수정 컨트롤러
     */
    @PatchMapping("/{timeTableId}/customSchedule/{customScheduleId}")
    public ResponseEntity<ResponseDto<TimeTableItemResponseDto>> updateTimeTableItemForCustom(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId,
            @PathVariable Long customScheduleId,
            @Valid @RequestBody TimeTableCustomItemRequestDto request
    ) {
        TimeTableItemResponseDto response =
                timeTableItemService.updateTimeTableItemForCustom(
                        member.getId(), timeTableId, customScheduleId, request
                );

        return ResponseEntity.ok(
                ResponseDto.of(response, "커스텀 일정 수정 성공")
        );
    }


    /**
     * 시간표 요소 삭제 컨트롤러
     */
    @DeleteMapping("/{timeTableId}/timeTableItem/{timeTableItemId}")
    public ResponseEntity<ResponseDto<Long>> deleteTimeTableItem(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId,
            @PathVariable Long timeTableItemId
    ) {
        timeTableItemService.deleteTimeTableItem(member.getId(), timeTableId, timeTableItemId);

        return ResponseEntity.ok(
                ResponseDto.of(timeTableItemId, "시간표 요소 삭제 성공")
        );
    }
}
