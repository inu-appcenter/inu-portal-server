-- 시간 맞추기 제출본. 한 사람이 한 조율에 낸 가능 시간이며, 이 줄이 있으면 참여자다. (3단계)
-- schedule_poll_create.sql 다음에 적용한다.
--
-- dev/prod는 ddl-auto가 none이라 테이블이 자동 생성되지 않는다. 서버 배포 전에 먼저 적용해야 한다.
-- MySQL 8 기준. 한 번만 실행한다.
CREATE TABLE IF NOT EXISTS schedule_poll_submission (
    id                           BIGINT       NOT NULL AUTO_INCREMENT,
    poll_id                      BIGINT       NOT NULL,
    member_id                    BIGINT       NOT NULL,
    -- 가능 시간 구간 목록 JSON. 예: [{"key":"MON","start":"09:00","end":"11:00"}]
    available                    TEXT         NOT NULL,
    -- 시간표에서 불러온 수업 ID 목록 JSON. 예: [4412,4413]
    imported_course_offering_ids TEXT         NULL,
    submission_revision          INT          NOT NULL,
    via                          VARCHAR(10)  NOT NULL,
    join_source                  VARCHAR(10)  NULL,
    joined_at                    DATETIME(6)  NOT NULL,
    submitted_at                 DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    -- 같은 사람이 동시에 첫 제출해도 한 줄만 생기게 한다.
    UNIQUE KEY uk_schedule_poll_submission_poll_member (poll_id, member_id),
    CONSTRAINT fk_schedule_poll_submission_poll FOREIGN KEY (poll_id) REFERENCES schedule_poll (id),
    CONSTRAINT fk_schedule_poll_submission_member FOREIGN KEY (member_id) REFERENCES member (id)
);
