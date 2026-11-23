-- 시간 맞추기: 개설자가 정한 후보 저장 (#538)
--
-- dev/prod는 ddl-auto가 none이라 칸이 자동으로 생기지 않는다. 서버 배포 전에 먼저 적용해야 한다.
-- schedule_poll_create.sql 적용 뒤에 실행. MySQL 8 기준. 한 번만 실행한다.
--
-- 모두 NULL 허용. 정한 적이 없는 조율(기존 조율 포함)은 전부 NULL이고, 한 번 정하면 바꾸지 않는다.
-- 열·시작·끝은 정한 때의 결과에서 복사한 값이다(조회 때 결과 JSON을 다시 읽지 않으려고).
ALTER TABLE schedule_poll
    ADD COLUMN decided_result_version INT          NULL AFTER deadline_processed_at,
    ADD COLUMN decided_candidate_rank INT          NULL AFTER decided_result_version,
    -- 정기면 요일('MON'), 일회성이면 날짜('2026-10-12')
    ADD COLUMN decided_key            VARCHAR(10)  NULL AFTER decided_candidate_rank,
    -- 하루 시작부터의 분 (range_start_min과 같은 방식, 24:00 → 1440)
    ADD COLUMN decided_start_min      INT          NULL AFTER decided_key,
    ADD COLUMN decided_end_min        INT          NULL AFTER decided_start_min,
    ADD COLUMN decided_at             DATETIME(6)  NULL AFTER decided_end_min;
