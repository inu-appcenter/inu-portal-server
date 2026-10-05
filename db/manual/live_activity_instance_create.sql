-- iOS 시간표 Live Activity의 갱신/종료를 서버가 하기 위한 테이블.
--
-- 앱이 꺼져 있으면 Activity를 갱신하거나 끝낼 수 없어, 끝난 수업의 Activity가 남아 다음 수업 것과 겹쳤다.
-- 앱이 Activity마다 ActivityKit 업데이트 push 토큰을 보고하면(PUT /api/tokens/live-activity/activities)
-- LiveActivityLifecycleScheduler가 수업 시작 시각에 "수업 중"으로 갱신하고 종료 시각에 Activity를 끝낸다.
--
-- dev/prod는 ddl-auto가 none이라 테이블이 자동 생성되지 않는다. 서버 배포 전에 먼저 적용해야 한다.
-- MySQL 8 기준. 한 번만 실행한다.
CREATE TABLE live_activity_instance (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    member_id     BIGINT        NOT NULL,
    activity_id   VARCHAR(64)   NOT NULL,
    fcm_token     VARCHAR(512)  NOT NULL,
    push_token    VARCHAR(512)  NOT NULL,
    props_json    VARCHAR(2048) NOT NULL,
    start_at      BIGINT        NOT NULL,
    end_at        BIGINT        NOT NULL,
    ongoing_sent  TINYINT(1)    NOT NULL DEFAULT 0,
    ended         TINYINT(1)    NOT NULL DEFAULT 0,
    modified_date DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_live_activity_instance_activity_id (activity_id),
    -- 1분마다 도는 스케줄러 조회(ended = false AND start_at <= now)용
    KEY idx_live_activity_instance_due (ended, start_at)
);
