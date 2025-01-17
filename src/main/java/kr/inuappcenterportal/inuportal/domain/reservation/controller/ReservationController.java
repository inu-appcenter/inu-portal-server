package kr.inuappcenterportal.inuportal.domain.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationCreate;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationDetail;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationPreview;
import kr.inuappcenterportal.inuportal.domain.reservation.service.ReservationService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "reservations", description = " 물품대여 API")
@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "예약 등록", description = "헤더 Auth에 발급받은 토큰을, 바디에 {startDateTime, endDateTime} ISO-8601 형식으로 보내주세요. ")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "예약 등록 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/{itemId}")
    public ResponseEntity<ResponseDto<Long>> register(@RequestBody @Valid ReservationCreate request, @PathVariable Long itemId,
                                                      @AuthenticationPrincipal Member member) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(reservationService.create(request, itemId, member.getId()), "예약 등록 성공"));
    }

    @Operation(summary = "예약 상세 조회", description = "헤더 Auth에 발급받은 토큰을 보내주세요. url 파라미터에 예약의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "예약 상세 조회 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/{reservationId}")
    public ResponseEntity<ResponseDto<ReservationDetail>> get(@PathVariable Long reservationId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(reservationService.get(reservationId), "예약 조회 성공"));
    }

    @Operation(summary = "예약 물품별 리스트 조회", description = "헤더 Auth에 발급받은 토큰을 보내주세요. url 파라미터에 물품의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "예약 물품별 리스트 조회 성공", content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
    })
    @GetMapping("/item/{itemId}")
    public ResponseEntity<ListResponseDto<ReservationPreview>> getListByItemId(@PathVariable Long itemId, @RequestParam(required = false,defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.getListByItemId(itemId, page));
    }

    @Operation(summary = "예약 삭제", description = "헤더 Auth에 발급받은 토큰을 보내주세요. url 파라미터에 물품의 id를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "예약 상세 조회 성공", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<ResponseDto<Long>> delete(@PathVariable Long itemId, @AuthenticationPrincipal Member member) {
        return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.of(reservationService.deleteByOwner(itemId, member.getId()), "예약 아이템별 리스트 조회 성공"));
    }

    @Operation(summary = "예약 리스트 조회", description = "헤더 Auth에 발급받은 토큰을 보내주세요. url 파라미터에 물품의 id를 보내주세요. 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "예약 상세 조회 성공", content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
    })
    @GetMapping
    public ResponseEntity<ListResponseDto<ReservationPreview>> getList(@RequestParam(required = false,defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.getList(page));
    }
}
