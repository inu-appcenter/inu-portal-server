-- 시간 맞추기 카드 공유 기록. (6단계)
-- schedule_poll_create.sql 다음에 적용한다.
--
-- dev/prod는 ddl-auto가 none이라 테이블이 자동 생성되지 않는다. 서버 배포 전에 먼저 적용해야 한다.
-- MySQL 8 기준. 한 번만 실행한다.
CREATE TABLE IF NOT EXISTS schedule_poll_share (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    poll_id         BIGINT       NOT NULL,
    kind            VARCHAR(10)  NOT NULL,
    -- RESULT 카드의 결과 번호. INVITE 카드는 0
    result_version  INT          NOT NULL,
    chat_room_id    BIGINT       NOT NULL,
    chat_message_id BIGINT       NOT NULL,
    -- 카드를 보낸 사람(메시지 발신자)
    shared_by       BIGINT       NOT NULL,
    create_date     DATETIME(6)  NULL,
    modified_date   DATETIME(6)  NULL,
    PRIMARY KEY (id),
    -- 같은 방에 같은 카드(조율·종류·버전)는 하나만
    UNIQUE KEY uk_schedule_poll_share_card_room (poll_id, kind, result_version, chat_room_id)
);

-- 채팅 메시지 종류 추가 확인 (SCHEDULE_POLL_INVITE, SCHEDULE_POLL_RESULT)
-- chat_message.messageType 칸이 ENUM 타입이면 새 종류를 저장할 때 오류가 난다. 먼저 타입을 확인한다.
--   SHOW COLUMNS FROM chat_message LIKE 'messageType';
-- Type이 varchar(...)면 할 일 없음. enum(...)이면 기존 값을 그대로 두고 아래처럼 두 값을 더한다.
--   ALTER TABLE chat_message MODIFY messageType
--     ENUM('TEXT','IMAGE','TIMETABLE_SHARE','BOT_QUESTION','BOT_ANSWER','SCHEDULE_POLL_INVITE','SCHEDULE_POLL_RESULT');
-- (SHOW COLUMNS 결과에 위 목록에 없는 값이 있으면 그 값도 반드시 넣는다.)
