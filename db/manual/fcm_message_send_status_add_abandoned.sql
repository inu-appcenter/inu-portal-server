-- fcm_message.send_status는 MySQL 네이티브 ENUM 타입이라, 애플리케이션 코드의
-- FcmSendStatus.ABANDONED 추가만으로는 DB에 이 값을 저장할 수 없다 (Data truncated 에러).
-- fcm_message_legacy_pending_cleanup.sql을 실행하기 전에 반드시 먼저 실행해야 한다.

ALTER TABLE fcm_message
    MODIFY COLUMN send_status
    ENUM('PENDING','PROCESSING','SUCCESS','PARTIAL_FAILURE','FAILED','NO_TARGET','ABANDONED')
    NOT NULL;
