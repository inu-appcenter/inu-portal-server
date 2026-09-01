-- 유실 보정 스케줄러가 재발행 시 라우팅 정보를 추정하지 않고 그대로 복원할 수 있도록,
-- prepareTrackedNotification()이 저장 시점에 path를 함께 영속화한다.

ALTER TABLE fcm_message
    ADD COLUMN IF NOT EXISTS path VARCHAR(255) NULL;
