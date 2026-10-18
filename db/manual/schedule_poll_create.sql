-- 시간 맞추기(Schedule Poll) 조율 1건을 담는 테이블. (1단계)
-- 참여자·제출본·결과·공유 기록 테이블은 각 기능 단계에서 별도 SQL로 추가한다.
--
-- dev/prod는 ddl-auto가 none이라 테이블이 자동 생성되지 않는다. 서버 배포 전에 먼저 적용해야 한다.
-- MySQL 8 기준. 한 번만 실행한다.
CREATE TABLE IF NOT EXISTS schedule_poll (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    creator_id            BIGINT       NOT NULL,
    title                 VARCHAR(30)  NOT NULL,
    is_recurring          TINYINT(1)   NOT NULL,
    -- 정기일 때 'MON,WED,FRI'(월→일 순). 일회성이면 NULL
    days                  VARCHAR(27)  NULL,
    -- 일회성일 때만 값이 있다.
    start_date            DATE         NULL,
    end_date              DATE         NULL,
    -- 하루 시작부터의 분. '24:00'을 담으려고 TIME 대신 숫자로 저장 (09:00 → 540, 24:00 → 1440)
    range_start_min       INT          NOT NULL,
    range_end_min         INT          NOT NULL,
    duration_mode         VARCHAR(10)  NOT NULL,
    duration_min          INT          NOT NULL,
    -- 서울 시각. NULL이면 마감 없음
    deadline_at           DATETIME(6)  NULL,
    origin_chat_room_id   BIGINT       NULL,
    link_token            VARCHAR(32)  NOT NULL,
    status                VARCHAR(16)  NOT NULL,
    -- 폴링 API가 조회 한 번으로 끝나도록 참여자 수를 같이 둔다.
    participant_count     INT          NOT NULL DEFAULT 0,
    revision              BIGINT       NOT NULL,
    result_version        INT          NOT NULL DEFAULT 0,
    closed_at             DATETIME(6)  NULL,
    -- 마감 1시간 전 알림 시각. 마감을 바꾸면 NULL로 초기화
    deadline_reminded_at  DATETIME(6)  NULL,
    -- 마감 처리 시각. 값이 있으면 스케줄러가 다시 처리하지 않는다.
    deadline_processed_at DATETIME(6)  NULL,
    create_date           DATETIME(6)  NULL,
    modified_date         DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_poll_link_token (link_token),
    -- 1분마다 도는 마감 스케줄러 조회(status <> CLOSED AND deadline_at <= now)용
    KEY idx_schedule_poll_due (status, deadline_at),
    CONSTRAINT fk_schedule_poll_creator FOREIGN KEY (creator_id) REFERENCES member (id)
);
