package kr.inuappcenterportal.inuportal.domain.firebase.enums;

public enum ScheduledNotificationStatus {
    /** 발송 대기 중. 스케줄러가 scheduledAt 도달 시 이 상태의 행만 선점 대상으로 본다. */
    SCHEDULED,
    /** 스케줄러가 선점해 발송 이벤트를 발행한 직후. 리스너가 최종 상태로 전이시킨다. */
    DISPATCHING,
    /** 발송 이벤트가 fcm_message로 이어졌다. 실제 성공/실패 결과는 fcmMessageId로 연결된
     * fcm_message.sendStatus를 따로 조회해야 한다 (이 상태는 "발화됐는가"만 의미한다). */
    SENT,
    /** 발송 시도 자체가 예외로 실패했다 (요청 역직렬화 실패 등). */
    FAILED,
    /** 관리자가 발송 전에 취소했다. */
    CANCELED,
    /** maxDelayMinutes를 초과해 도달한 예약. 시의성이 지나 발송하지 않고 종결한다. */
    EXPIRED
}
