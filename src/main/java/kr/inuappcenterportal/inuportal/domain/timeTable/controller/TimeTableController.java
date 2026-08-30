package kr.inuappcenterportal.inuportal.domain.timeTable.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable.TimeTableCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable.TimeTableNameUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable.TimeTableVisibilityUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.ChatRoomTimeTableResponseDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetables")
public class TimeTableController implements TimeTableApiSpecification {

    private final TimeTableService timeTableService;


    /**
     * 내 시간표 상세 조회 컨트롤러
     */
    @GetMapping("/{timeTableId}")
    public ResponseEntity<ResponseDto<TimeTableDetailResponseDto>> getTimeTableDetail(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId
    ) {
        TimeTableDetailResponseDto response =
                timeTableService.getTimeTableDetail(member.getId(), timeTableId);

        return ResponseEntity.ok(
                ResponseDto.of(response, "시간표 상세 조회 성공")
        );
    }

    /**
     * 친구 대표 시간표 학기별 조회 컨트롤러
     */
    @GetMapping("/friends/{friendMemberId}/primary")
    public ResponseEntity<ResponseDto<TimeTableDetailResponseDto>> getFriendPrimarySemesterTimeTable(
            @AuthenticationPrincipal Member member,
            @PathVariable Long friendMemberId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) SemesterTerm term
    ) {
        TimeTableDetailResponseDto response =
                timeTableService.getFriendPrimaryTimeTableDetailYearAndTerm(member.getId(), friendMemberId, year, term);

        return ResponseEntity.ok(
                ResponseDto.of(response, "친구 대표 시간표 상세 조회 성공")
        );
    }

    @GetMapping("/chat-rooms/{roomId}/primary")
    public ResponseEntity<ResponseDto<List<ChatRoomTimeTableResponseDto>>> getChatRoomPrimaryTimeTables(
            @AuthenticationPrincipal Member member,
            @PathVariable Long roomId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) SemesterTerm term
    ) {
        return ResponseEntity.ok(ResponseDto.of(
                timeTableService.getChatRoomPrimaryTimeTables(member.getId(), roomId, year, term),
                "단체톡 참여자 대표 시간표 조회 성공"));
    }

    /**
     * 시간표 조회 컨트롤러
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<TimeTableResponseDto>>> getTimeTables(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) SemesterTerm term
    ) {
        List<TimeTableResponseDto> response;

        if (year == null && term == null) {
            response = timeTableService.getTimeTables(member.getId());
        } else {
            response = timeTableService.getTimeTablesOfYearAndTerm(member.getId(), year, term);
        }

        return ResponseEntity.ok(
                ResponseDto.of(response, "시간표 조회 성공")
        );
    }

    /**
     * 학기별 시간표 조회 컨트롤러(id)
     */
    @GetMapping("/semesters/{semesterId}")
    public ResponseEntity<ResponseDto<List<TimeTableResponseDto>>> getTimeTablesOfSemester(
            @AuthenticationPrincipal Member member,
            @PathVariable Long semesterId
    ) {
        List<TimeTableResponseDto> response = timeTableService.getTimeTablesOfSemester(member.getId(), semesterId);

        return ResponseEntity.ok(
                ResponseDto.of(response, "학기별 시간표(id) 조회 성공")
        );
    }


    /**
     * 시간표 이름 수정 컨트롤러
     */
    @PatchMapping("/{timeTableId}/timeTableName")
    public ResponseEntity<ResponseDto<TimeTableResponseDto>> setTimeTableName(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId,
            @RequestBody @Valid TimeTableNameUpdateRequestDto request
    ) {
        TimeTableResponseDto response =
                timeTableService.setTimeTableName(member.getId(), timeTableId, request);

        return ResponseEntity.ok(
                ResponseDto.of(response, "시간표 이름 변경 성공")
        );
    }


    /**
     * 대표 시간표 수정 컨트롤러
     */
    @PatchMapping("/{timeTableId}/isPrimary")
    public ResponseEntity<ResponseDto<TimeTableResponseDto>> setIsPrimary(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId
    ) {
        TimeTableResponseDto response =
                timeTableService.setIsPrimary(member.getId(), timeTableId);

        return ResponseEntity.ok(
                ResponseDto.of(response, "대표 시간표 변경 성공")
        );
    }


    /**
     * 시간표 공개범위 수정 컨트롤러
     */
    @PatchMapping("/{timeTableId}/visibility")
    public ResponseEntity<ResponseDto<TimeTableResponseDto>> setVisibility(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId,
            @RequestBody @Valid TimeTableVisibilityUpdateRequestDto request
    ) {
        TimeTableResponseDto response =
                timeTableService.setVisibility(member.getId(), timeTableId, request);

        return ResponseEntity.ok(
                ResponseDto.of(response, "시간표 공개 범위 변경 성공")
        );
    }


    /**
     * 시간표 삭제 메서드
     */
    @DeleteMapping("/{timeTableId}")
    public ResponseEntity<ResponseDto<Long>> deleteTimeTable(
            @AuthenticationPrincipal Member member,
            @PathVariable Long timeTableId
    ) {
        timeTableService.deleteTimeTable(member.getId(), timeTableId);

        return ResponseEntity.ok(
                ResponseDto.of(timeTableId, "시간표 삭제 성공")
        );
    }


    /**
     * 시간표 생성 메서드
     */
    @PostMapping("/semesters/{semesterId}")
    public ResponseEntity<ResponseDto<TimeTableResponseDto>> createTimeTable(
            @AuthenticationPrincipal Member member,
            @PathVariable Long semesterId,
            @RequestBody @Valid TimeTableCreateRequestDto request
    ) {
        TimeTableResponseDto response = timeTableService.createTimeTable(member.getId(), semesterId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseDto.of(response, "시간표 생성 성공")
        );
    }

}
