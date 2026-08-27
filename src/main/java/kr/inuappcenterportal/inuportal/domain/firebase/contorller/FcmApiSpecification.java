package kr.inuappcenterportal.inuportal.domain.firebase.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.TokenRequestDto;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.AdminNotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.NotificationResponse;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Tokens", description = "Fcm 토큰 API")
public interface FcmApiSpecification {

    @Operation(summary = "FCM 토큰 등록", description = "FCM 토큰을 등록하거나 기존 토큰의 회원/기기 정보를 갱신합니다.")
    ResponseEntity<ResponseDto<Long>> saveToken(
            @Valid TokenRequestDto tokenRequestDto,
            @Parameter(hidden = true) Member member
    );

    @Operation(summary = "FCM 토큰 회원 정보 삭제", description = "로그인한 회원의 FCM 토큰 연결을 해제합니다.")
    ResponseEntity<ResponseDto<Long>> deleteToken(
            @Valid TokenRequestDto tokenRequestDto,
            @Parameter(hidden = true) Member member
    );

    @Operation(summary = "회원의 받은 알림 조회", description = "회원이 받은 모든 알림을 최신순으로 조회합니다.")
    ResponseEntity<ResponseDto<ListResponseDto<NotificationResponse>>> checkNotification(
            @Parameter(hidden = true) Member member,
            @Min(1) int page
    );

    @Operation(summary = "단건 알림 읽음 처리", description = "특정 알림 항목을 읽음 상태로 처리합니다.")
    ResponseEntity<ResponseDto<Void>> readNotification(
            @Parameter(hidden = true) Member member,
            Long memberFcmMessageId
    );

    @Operation(summary = "푸시 알림 식별자로 읽음 처리",
            description = "푸시 payload의 <code>fcmMessageId</code>로 해당 알림을 읽음 상태로 처리합니다. <br><br>" +
                    "payload에는 수신자 전체가 공유하는 식별자만 담기며, 개인 알림함 항목은 인증된 회원 정보와 조합해 서버가 특정합니다. <br>" +
                    "알림함 목록에서 항목을 직접 누른 경우에는 <code>memberFcmMessageId</code>를 쓰는 단건 읽음 처리 API를 사용하세요.")
    ResponseEntity<ResponseDto<Void>> readNotificationByFcmMessageId(
            @Parameter(hidden = true) Member member,
            Long fcmMessageId
    );

    @Operation(summary = "페이지 알림 읽음 처리", description = "회원의 특정 알림 페이지에 포함된 알림을 읽음 상태로 처리합니다.")
    ResponseEntity<ResponseDto<Void>> readPageNotification(
            @Parameter(hidden = true) Member member,
            @Min(1) int page
    );

    @Operation(summary = "전체 알림 읽음 처리", description = "회원의 모든 알림을 읽음 상태로 처리합니다.")
    ResponseEntity<ResponseDto<Integer>> readAllNotification(
            @Parameter(hidden = true) Member member
    );

    @Operation(summary = "안 읽은 알림 존재 여부 확인", description = "회원의 안 읽은 알림이 존재하는지 확인합니다.")
    ResponseEntity<ResponseDto<Boolean>> checkUnreadStatus(
            @Parameter(hidden = true) Member member
    );

    @Operation(summary = "(관리자 전용) 회원 알림 전송",
            description = "지정 회원들에게 알림을 전송합니다. <br><br>" +
                    "memberIds가 비어있으면 전체 회원에게 알림을 전송합니다.")
    ResponseEntity<ResponseDto<Long>> sendToMembers(@Valid AdminNotificationRequest request);

    @Operation(summary = "(관리자 전용) 관리자 전송 FCM 메시지 성공 횟수 조회",
            description = "관리자가 전송한 FCM 메세지들의 총 성공 횟수를 조회합니다.")
    ResponseEntity<ResponseDto<List<AdminNotificationResponse>>> countAdminFcmMessagesSuccess(@Min(1) int page);

    @Operation(summary = "(관리자 전용) 관리자 전송 FCM 메시지 결과 조회",
            description = "관리자가 전송한 FCM 메시지의 발송 결과를 조회합니다.")
    ResponseEntity<ResponseDto<AdminNotificationResponse>> getAdminFcmMessageResult(Long fcmMessageId);
}
