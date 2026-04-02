package kr.inuappcenterportal.inuportal.domain.firebase.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.AdminNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.TokenRequestDto;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.AdminNotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.NotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tokens", description = "Fcm ?좏겙 API")
@Slf4j
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;
    private final FcmAsyncService fcmAsyncService;

    @PostMapping("")
    public ResponseEntity<ResponseDto<Long>> saveToken(@Valid @RequestBody TokenRequestDto tokenRequestDto,
                                                       @AuthenticationPrincipal Member member) {
        fcmService.saveToken(tokenRequestDto, member == null ? null : member.getId());
        return ResponseEntity.ok(ResponseDto.of(1L, "토큰 등록 성공"));
    }

    @DeleteMapping("")
    public ResponseEntity<ResponseDto<Long>> deleteToken(@Valid @RequestBody TokenRequestDto tokenRequestDto,
                                                         @AuthenticationPrincipal Member member) {
        fcmService.deleteToken(tokenRequestDto.getToken(), member == null ? null : member.getId());
        return ResponseEntity.ok(ResponseDto.of(1L, "토큰에서 회원 정보 삭제 성공"));
    }

    @Operation(summary = "회원의 받은 알림 조회", description = "회원이 받은 모든 알림을 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ResponseDto<ListResponseDto<NotificationResponse>>> checkNotification(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page
    ) {
        return ResponseEntity.ok(ResponseDto.of(fcmService.findNotifications(member, page), "알림 조회 성공"));
    }

    @Operation(summary = "(관리자 전용) 회원 알림 전송",
            description = "지정 회원들에게 알림을 전송합니다. <br><br>" +
                    "memberIds가 비어있으면 전체 회원에게 알림을 전송합니다.")
    @PostMapping("/admin")
    public ResponseEntity<ResponseDto<Long>> sendToMembers(@Valid @RequestBody AdminNotificationRequest request) {
        AdminNotificationDispatch dispatch = fcmService.prepareAdminNotification(request);

        if (dispatch.hasTarget() || dispatch.hasMemberTarget()) {
            fcmAsyncService.sendAsyncToMembers(dispatch);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseDto.of(dispatch.fcmMessageId(), "FCM 발송 요청 접수 성공"));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.of(dispatch.fcmMessageId(), "발송 대상 토큰이 없어 요청만 기록했습니다."));
    }

    @Operation(summary = "(관리자 전용) 관리자 전송 FCM 메시지 성공 횟수 조회",
            description = "관리자가 전송한 FCM 메세지들의 총 성공 횟수를 조회합니다.")
    @GetMapping("/admin")
    public ResponseEntity<ResponseDto<List<AdminNotificationResponse>>> countAdminFcmMessagesSuccess(
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page
    ) {
        return ResponseEntity.ok(ResponseDto.of(fcmService.countAdminFcmMessagesSuccess(page), "FCM 메시지 개수 조회 성공"));
    }

    @GetMapping("/admin/{fcmMessageId}")
    public ResponseEntity<ResponseDto<AdminNotificationResponse>> getAdminFcmMessageResult(@PathVariable Long fcmMessageId) {
        return ResponseEntity.ok(ResponseDto.of(fcmService.findAdminNotificationResult(fcmMessageId), "FCM 메시지 조회 성공"));
    }
}
