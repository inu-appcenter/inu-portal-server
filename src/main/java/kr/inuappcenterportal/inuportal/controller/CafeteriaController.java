package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.CafeteriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name="Cafeterias", description = "학식 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/cafeterias")
public class CafeteriaController {
    private final CafeteriaService cafeteriaService;
    @Operation(summary = "학식 메뉴 가져오기",description = "url 파라미터에 식당 이름(학생식당, 제1기숙사식당, 2호관(교직원)식당, 27호관식당, 사범대식당)과 요일(월요일 : 1 ~ 일요일 : 7)을 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "학식 메뉴 가져오기 성공",content = @Content(schema = @Schema(implementation = NoticeListResponseDto.class))),
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<List<String>>> getMenu(@RequestParam String cafeteria,@RequestParam(defaultValue = "0") Integer day){
        return ResponseEntity.ok(ResponseDto.of(cafeteriaService.getCafeteria(cafeteria,day),"학식 메뉴 가져오기 성공"));
    }
}
