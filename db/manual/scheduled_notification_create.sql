-- 어드민 예약 푸시. fcm_message와 분리해 "실제 발송이 시도된 메시지"라는 fcm_message의
-- 의미를 유지하고, PENDING 기반 유실 보정 스케줄러(FcmStalledNotificationScheduler)와의
-- 간섭을 구조적으로 차단한다. 발송 시점에 prepareAdminNotification()이 비로소
-- fcm_message를 만들고, 그 id를 fcm_message_id에 (느슨하게) 링크한다.
--
-- dev/prod는 spring.jpa.hibernate.ddl-auto가 설정돼 있지 않아(Hibernate 기본값 none)
-- 테이블이 자동 생성되지 않는다. 반드시 앱 배포 전에 이 SQL을 먼저 적용해야 하며,
-- fcm.schedule.enabled 기본값이 false라 배포 순서가 어긋나도 즉시 장애로 이어지지는
-- 않는다(스케줄러가 켜지기 전까지는 이 테이블을 아무도 읽지 않는다).

CREATE TABLE IF NOT EXISTS scheduled_notification (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    title            VARCHAR(255) NOT NULL,
    content          TEXT         NOT NULL,
    path             VARCHAR(512) NULL,
    target_type      VARCHAR(32)  NOT NULL,
    sub_filter       VARCHAR(32)  NOT NULL,
    -- AdminNotificationRequest 원본 JSON. 발송 대상은 예약 시점이 아니라 발송 시점에
    -- 이 조건으로 다시 조회한다 (그 사이의 가입/탈퇴/토큰 변경을 반영하기 위함).
    request_payload  TEXT         NOT NULL,
    scheduled_at     DATETIME     NOT NULL,
    status           VARCHAR(32)  NOT NULL DEFAULT 'SCHEDULED',
    fcm_message_id   BIGINT       NULL,
    failure_reason   VARCHAR(512) NULL,
    create_date      DATETIME     NULL,
    modified_date    DATETIME     NULL,
    PRIMARY KEY (id),
    -- findDueIds(status = SCHEDULED AND scheduled_at <= now ORDER BY scheduled_at)가
    -- 1분마다 도는 쿼리라 복합 인덱스가 필요하다.
    KEY idx_scheduled_notification_due (status, scheduled_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
