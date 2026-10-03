package kr.inuappcenterportal.inuportal.domain.firebase.dto;

/**
 * 갱신 또는 종료 시점이 된 Live Activity. 트랜잭션 밖에서 발송할 수 있도록 엔티티 없이 값만 담는다.
 */
public record DueLiveActivity(
        Long id,
        String fcmToken,
        String pushToken,
        String propsJson,
        long startAt,
        long endAt,
        boolean ongoingSent
) {
}
