-- 시간 맞추기 결과 버전과 제출 요청 기록. (4단계)
-- schedule_poll_create.sql, schedule_poll_submission_create.sql 다음에 적용한다.
--
-- dev/prod는 ddl-auto가 none이라 테이블이 자동 생성되지 않는다. 서버 배포 전에 먼저 적용해야 한다.
-- MySQL 8 기준. 한 번만 실행한다.

-- 결과 1버전. 내용은 바뀌지 않고 state(LATEST → STALE)와 is_final만 바뀐다.
CREATE TABLE IF NOT EXISTS schedule_poll_result (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    poll_id                BIGINT       NOT NULL,
    version                INT          NOT NULL,
    state                  VARCHAR(10)  NOT NULL,
    is_final               TINYINT(1)   NOT NULL DEFAULT 0,
    based_on_submitted_at  DATETIME(6)  NOT NULL,
    -- 참여자 회원 ID 목록 JSON. 순서가 히트맵 자리(index)
    participant_member_ids TEXT         NOT NULL,
    -- heatmap[열][칸] = 가능한 참여자 비트마스크
    heatmap                TEXT         NOT NULL,
    -- 추천 후보(최대 3개) JSON
    candidates             TEXT         NOT NULL,
    create_date            DATETIME(6)  NULL,
    modified_date          DATETIME(6)  NULL,
    PRIMARY KEY (id),
    -- 결과 버전 중복 방지
    UNIQUE KEY uk_schedule_poll_result_poll_version (poll_id, version),
    KEY idx_schedule_poll_result_poll_state (poll_id, state),
    CONSTRAINT fk_schedule_poll_result_poll FOREIGN KEY (poll_id) REFERENCES schedule_poll (id)
);

-- 처리한 제출 요청. 같은 client_submission_id가 다시 오면 처음 응답을 그대로 돌려준다.
CREATE TABLE IF NOT EXISTS schedule_poll_submit_request (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    client_submission_id VARCHAR(36)  NOT NULL,
    poll_id              BIGINT       NOT NULL,
    member_id            BIGINT       NOT NULL,
    response_json        TEXT         NOT NULL,
    create_date          DATETIME(6)  NULL,
    modified_date        DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_poll_submit_request_client_id (client_submission_id)
);
