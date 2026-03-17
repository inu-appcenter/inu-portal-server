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
        return ResponseEntity.ok(ResponseDto.of(1L, "?좏겙 ?깅줉 ?깃났"));
    }

    @DeleteMapping("")
    public ResponseEntity<ResponseDto<Long>> deleteToken(@Valid @RequestBody TokenRequestDto tokenRequestDto,
                                                         @AuthenticationPrincipal Member member) {
        fcmService.deleteToken(tokenRequestDto.getToken(), member == null ? null : member.getId());
        return ResponseEntity.ok(ResponseDto.of(1L, "?좏겙?먯꽌 ?뚯썝 ?뺣낫 ??젣 ?깃났"));
    }

    @Operation(summary = "?뚯썝??諛쏆? ?뚮┝ 議고쉶", description = "?뚯썝??諛쏆? 紐⑤뱺 ?뚮┝??理쒖떊?쒖쑝濡?議고쉶?⑸땲??")
    @GetMapping
    public ResponseEntity<ResponseDto<ListResponseDto<NotificationResponse>>> checkNotification(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page
    ) {
        return ResponseEntity.ok(ResponseDto.of(fcmService.findNotifications(member, page), "?뚮┝ 議고쉶 ?깃났"));
    }

    @Operation(summary = "(愿由ъ옄 ?꾩슜) ?뚯썝 ?뚮┝ ?꾩넚",
            description = "吏???뚯썝?ㅼ뿉寃??뚮┝???꾩넚?⑸땲?? <br><br>" +
                    "memberIds媛 鍮꾩뼱?덉쑝硫??꾩껜 ?뚯썝?먭쾶 ?뚮┝???꾩넚?⑸땲??")
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

    @Operation(summary = "(愿由ъ옄 ?꾩슜) 愿由ъ옄 ?꾩넚 FCM 硫붿떆吏 ?깃났 ?잛닔 議고쉶",
            description = "愿由ъ옄媛 ?꾩넚??FCM 硫붿꽭吏?ㅼ쓽 珥??깃났 ?잛닔瑜?議고쉶?⑸땲??")
    @GetMapping("/admin")
    public ResponseEntity<ResponseDto<List<AdminNotificationResponse>>> countAdminFcmMessagesSuccess(
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page
    ) {
        return ResponseEntity.ok(ResponseDto.of(fcmService.countAdminFcmMessagesSuccess(page), "FCM 硫붿떆吏 媛쒖닔 議고쉶 ?깃났"));
    }

    @GetMapping("/admin/{fcmMessageId}")
    public ResponseEntity<ResponseDto<AdminNotificationResponse>> getAdminFcmMessageResult(@PathVariable Long fcmMessageId) {
        return ResponseEntity.ok(ResponseDto.of(fcmService.findAdminNotificationResult(fcmMessageId), "FCM 메시지 조회 성공"));
    }
}
