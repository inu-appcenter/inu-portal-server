package kr.inuappcenterportal.inuportal.domain.timeTable.controller;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetables")
public class TimeTableController {

    private final TimeTableService timeTableService;

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

}
