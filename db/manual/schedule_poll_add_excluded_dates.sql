-- 시간 맞추기: 일회성 조율에서 기간 안의 특정 날짜를 빼는 기능 (excludedDates)
--
-- dev/prod는 ddl-auto가 none이라 칸이 자동으로 생기지 않는다. 서버 배포 전에 먼저 적용해야 한다.
-- schedule_poll_create.sql 적용 뒤에 실행. MySQL 8 기준. 한 번만 실행한다.
--
-- 함께 바뀐 뜻(칸 변경 없음):
--   days 칸은 이제 일회성 조율에서도 값이 있을 수 있다. 일회성이면 기간 중 이 요일만 열로 쓰고, NULL이면 모든 요일.
--   기존 일회성 조율은 days·excluded_dates가 NULL이라 지금과 똑같이 보인다.

-- 일회성일 때 기간 안에서 뺄 날짜. '2026-11-11,2026-11-12'(날짜순). NULL이면 빼지 않음 (최대 14일 × 11자 = 154자)
ALTER TABLE schedule_poll ADD COLUMN excluded_dates VARCHAR(160) NULL AFTER end_date;
