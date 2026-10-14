-- 시간 맞추기 결과를 내 시간표에 추가한 이력. (7단계)
-- 한 결과(조율·버전)에서는 한 사람당 후보 하나만 추가된다.
-- schedule_poll_create.sql 다음에 적용한다.
--
-- dev/prod는 ddl-auto가 none이라 테이블이 자동 생성되지 않는다. 서버 배포 전에 먼저 적용해야 한다.
-- MySQL 8 기준. 한 번만 실행한다.
CREATE TABLE IF NOT EXISTS schedule_poll_timetable_add (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    member_id          BIGINT       NOT NULL,
    poll_id            BIGINT       NOT NULL,
    version            INT          NOT NULL,
    candidate_rank     INT          NOT NULL,
    -- 만든 시간표 요소와 커스텀 일정. 사용자가 시간표에서 지우면 요소가 없어진다.
    timetable_item_id  BIGINT       NOT NULL,
    custom_schedule_id BIGINT       NOT NULL,
    create_date        DATETIME(6)  NULL,
    modified_date      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    -- 시간표 추가 연타 방지
    UNIQUE KEY uk_schedule_poll_timetable_add_member_poll_version (member_id, poll_id, version)
);
