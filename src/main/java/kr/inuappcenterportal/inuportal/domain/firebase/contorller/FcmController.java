package kr.inuappcenterportal.inuportal.domain.firebase.contorller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.AdminNotificationDispatch;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.TokenRequestDto;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.AdminNotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.NotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.ScheduledNotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.model.ScheduledNotification;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.ScheduledNotificationService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class FcmController implements FcmApiSpecification {

    private final FcmService fcmService;
    private final FcmAsyncService fcmAsyncService;
    private final ScheduledNotificationService scheduledNotificationService;

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

    @GetMapping
    public ResponseEntity<ResponseDto<ListResponseDto<NotificationResponse>>> checkNotification(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false, defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(ResponseDto.of(fcmService.findNotifications(member, page), "알림 조회 성공"));
    }

    @PatchMapping("/notifications/{memberFcmMessageId}/read")
    public ResponseEntity<ResponseDto<Void>> readNotification(
            @AuthenticationPrincipal Member member,
            @PathVariable Long memberFcmMessageId
    ) {
        fcmService.markNotificationAsRead(member, memberFcmMessageId);
        return ResponseEntity.ok(ResponseDto.of(null, "알림 읽음 처리 성공"));
    }

    @PatchMapping("/notifications/fcm-messages/{fcmMessageId}/read")
    public ResponseEntity<ResponseDto<Void>> readNotificationByFcmMessageId(
            @AuthenticationPrincipal Member member,
            @PathVariable Long fcmMessageId
    ) {
        fcmService.markNotificationAsReadByFcmMessageId(member, fcmMessageId);
        return ResponseEntity.ok(ResponseDto.of(null, "알림 읽음 처리 성공"));
    }

    @PatchMapping("/notification/read")
    public ResponseEntity<ResponseDto<Void>> readPageNotification(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false, defaultValue = "1") int page
    ) {
        fcmService.markPageNotificationAsRead(member, page);
        return ResponseEntity.ok(ResponseDto.of(null, "해당 페이지 알림 읽음 처리 성공"));
    }

    @PatchMapping("/notification/read-all")
    public ResponseEntity<ResponseDto<Integer>> readAllNotification(
            @AuthenticationPrincipal Member member
    ) {
        int updatedCount = fcmService.markAllNotificationAsRead(member);
        return ResponseEntity.ok(ResponseDto.of(updatedCount, "전체 알림 읽음 처리 성공"));
    }

    @GetMapping("/notification/not-read")
    public ResponseEntity<ResponseDto<Integer>> getIsReadFalseNotification(
            @AuthenticationPrincipal Member member
    ) {
        int count = fcmService.findIsReadFalseNotification(member);
        return ResponseEntity.ok(ResponseDto.of(count, "읽지 않은 알림 갯수 조회 성공"));
    }

    @GetMapping("/unread-status")
    public ResponseEntity<ResponseDto<Boolean>> checkUnreadStatus(
            @AuthenticationPrincipal Member member
    ) {
        boolean hasUnread = fcmService.hasUnreadNotification(member);
        return ResponseEntity.ok(ResponseDto.of(hasUnread, "안 읽은 알림 상태 조회 성공"));
    }

    @PostMapping("/admin")
    public ResponseEntity<ResponseDto<Long>> sendToMembers(@Valid @RequestBody AdminNotificationRequest request) {
        if (request.scheduledAt() != null) {
            ScheduledNotification scheduledNotification = scheduledNotificationService.reserve(request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseDto.of(scheduledNotification.getId(), "FCM 예약 등록 성공"));
        }

        AdminNotificationDispatch dispatch = fcmService.prepareAdminNotification(request);

        if (dispatch.hasTarget() || dispatch.hasMemberTarget()) {
            fcmAsyncService.sendAsyncToMembers(dispatch);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseDto.of(dispatch.fcmMessageId(), "FCM 발송 요청 접수 성공"));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.of(dispatch.fcmMessageId(), "발송 대상 토큰이 없어 요청만 기록했습니다."));
    }

    @GetMapping("/admin/scheduled")
    public ResponseEntity<ResponseDto<ListResponseDto<ScheduledNotificationResponse>>> getScheduledNotifications(
            @RequestParam(required = false, defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(ResponseDto.of(scheduledNotificationService.findScheduled(page), "예약 알림 조회 성공"));
    }

    @DeleteMapping("/admin/scheduled/{scheduledNotificationId}")
    public ResponseEntity<ResponseDto<Void>> cancelScheduledNotification(@PathVariable Long scheduledNotificationId) {
        scheduledNotificationService.cancel(scheduledNotificationId);
        return ResponseEntity.ok(ResponseDto.of(null, "예약 알림 취소 성공"));
    }

    @GetMapping("/admin")
    public ResponseEntity<ResponseDto<List<AdminNotificationResponse>>> countAdminFcmMessagesSuccess(
            @RequestParam(required = false, defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(ResponseDto.of(fcmService.countAdminFcmMessagesSuccess(page), "FCM 메시지 개수 조회 성공"));
    }

    @GetMapping("/admin/{fcmMessageId}")
    public ResponseEntity<ResponseDto<AdminNotificationResponse>> getAdminFcmMessageResult(@PathVariable Long fcmMessageId) {
        return ResponseEntity.ok(ResponseDto.of(fcmService.findAdminNotificationResult(fcmMessageId), "FCM 메시지 조회 성공"));
    }
}
