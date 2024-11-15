package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.dto.ScheduleResponseDto;
import kr.inuappcenterportal.inuportal.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name="Schedules", description = "학사일정 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/schedules")
public class ScheduleController {
    private final ScheduleService scheduleService;
    @Operation(summary = "학사일정 가져오기",description = "url 파라미터에 년(year), 월(month)을 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "학사일정 가져오기 성공",content = @Content(schema = @Schema(implementation = NoticeListResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<List<ScheduleResponseDto>>> getScheduleByMonth(@RequestParam int year,@RequestParam int month){
        return ResponseEntity.ok(ResponseDto.of(scheduleService.getScheduleByMonth(year,month),"학사일정 가져오기 성공"));
    }
}
