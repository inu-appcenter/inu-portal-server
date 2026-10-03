package kr.inuappcenterportal.inuportal.domain.firebase.dto.res;

/**
 * 발송 로그 한 건의 읽음/클릭 집계 (#466).
 *
 * @param recipientCount 알림함 행이 만들어진 수신 회원 수. 클릭율의 분모다.
 *                       fcm_message의 targetCount/sendCount는 토큰(기기) 단위라 분모로 쓰면
 *                       기기 여러 대를 쓰는 회원 때문에 전환율이 낮게 나온다.
 * @param readCount      읽음으로 바뀐 행 수. 전체 읽음/자동 읽음까지 포함한다.
 * @param pushReadCount  푸시를 직접 탭해 읽은 수.
 * @param inboxReadCount 알림함에서 개별 항목을 눌러 읽은 수.
 */
public record NotificationReadStats(
        int recipientCount,
        int readCount,
        int pushReadCount,
        int inboxReadCount
) {
    private static final NotificationReadStats EMPTY = new NotificationReadStats(0, 0, 0, 0);

    public static NotificationReadStats empty() {
        return EMPTY;
    }

    /** 실제 클릭 수. 일괄 읽음(BULK)과 read_source 도입 이전 데이터는 빠진다. */
    public int clickCount() {
        return pushReadCount + inboxReadCount;
    }
}
