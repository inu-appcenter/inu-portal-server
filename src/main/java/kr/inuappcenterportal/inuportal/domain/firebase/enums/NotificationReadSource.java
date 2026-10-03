package kr.inuappcenterportal.inuportal.domain.firebase.enums;

/**
 * 알림을 읽음 처리한 경로. 발송별 클릭율 집계(#466)의 분모/분자를 가르는 기준이다.
 *
 * <p>PUSH/INBOX는 사용자가 알림 하나를 직접 열어본 "클릭"이지만, BULK는 전체 읽음이나
 * 조회수 기반 자동 읽음이라 클릭으로 셀 수 없다. 셋을 한 컬럼으로 구분해 두지 않으면
 * is_read만으로는 실제 클릭과 일괄 처리가 뒤섞여 전환율이 부풀려진다.
 */
public enum NotificationReadSource {

    /** 푸시 알림을 탭해 열었다. {@code PATCH /notifications/fcm-messages/{fcmMessageId}/read} */
    PUSH,

    /** 앱 알림함에서 개별 알림을 눌러 열었다. {@code PATCH /notifications/{memberFcmMessageId}/read} */
    INBOX,

    /** 전체/페이지 읽음, 조회수 기반 자동 읽음. 클릭으로 집계하지 않는다. */
    BULK;

    /** 사용자가 알림 하나를 직접 열어본 경로인지. 클릭율의 분자에 들어가는 값이다. */
    public boolean isClick() {
        return this == PUSH || this == INBOX;
    }
}
