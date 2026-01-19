package kr.inuappcenterportal.inuportal.domain.firebase.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

@Tag(name = "Tokens", description = "Fcm 토큰 API")
@Slf4j
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;
    private final FcmAsyncService fcmAsyncService;

    @PostMapping("")
    public ResponseEntity<ResponseDto<Long>> saveToken(@Valid @RequestBody TokenRequestDto tokenRequestDto, @AuthenticationPrincipal Member member){
        fcmService.saveToken(tokenRequestDto.getToken(), member==null?null:member.getId());
        return ResponseEntity.ok(ResponseDto.of(1L,"토큰 등록 성공"));
    }

    @DeleteMapping("")
    public ResponseEntity<ResponseDto<Long>> deleteToken(@AuthenticationPrincipal Member member){
        fcmService.deleteToken( member.getId());
        return ResponseEntity.ok(ResponseDto.of(1L,"토큰에서 회원 정보 삭제 성공"));
    }

    // 받은 알림 조회
    @Operation(summary = "회원의 받은 알림 조회", description = "회원이 받은 모든 알림을 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ResponseDto<ListResponseDto<NotificationResponse>>> checkNotification(@AuthenticationPrincipal Member member,
                                                                                                @RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        return ResponseEntity.ok(ResponseDto.of(fcmService.findNotifications(member, page),"알림 조회 성공"));
    }

    @Operation(summary = "(관리자 전용) 회원 알림 전송",
            description = "지정 회원들에게 알림을 전송합니다. <br><br>" +
                    "memberIds가 비어있으면 전체 회원에게 알림을 전송합니다.")
    @PostMapping("/admin")
    public ResponseEntity<ResponseDto<Long>> sendToMembers(@Valid @RequestBody AdminNotificationRequest request){
        fcmAsyncService.sendAsyncToMembers(request);
        if (request.memberIds() == null || request.memberIds().isEmpty())
            return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.of(1L, "전체 회원 알림 전송 성공"));
        else
            return ResponseEntity.status(HttpStatus.OK).body(ResponseDto.of(1L, "지정 회원 알림 전송 성공"));
    }

    @Operation(summary = "(관리자 전용) 관리자 전송 FCM 메시지 성공 횟수 조회",
            description = "관리자가 전송한 FCM 메세지들의 총 성공 횟수를 조회합니다.")
    @GetMapping("/admin")
    public ResponseEntity<ResponseDto<List<AdminNotificationResponse>>> countAdminFcmMessagesSuccess(
            @RequestParam(required = false,defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(ResponseDto.of(fcmService.countAdminFcmMessagesSuccess(page), "FCM 메시지 개수 조회 성공"));
    }
}