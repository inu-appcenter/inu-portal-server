package kr.inuappcenterportal.inuportal.domain.firebase.enums;

public enum FcmSendStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    PARTIAL_FAILURE,
    FAILED,
    NO_TARGET,
    /**
     * send_status 컬럼 도입(5f8e2483) 이전에 생성된 행에 기본값 PENDING이 소급 backfill된 것.
     * 실제로 발송 대기 중이었던 적이 없으므로, 유실 보정 스케줄러 등 PENDING을 재처리 대상으로
     * 보는 로직이 건드리지 않도록 별도 상태로 격리한다.
     */
    ABANDONED
}
