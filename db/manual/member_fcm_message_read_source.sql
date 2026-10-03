-- 발송별 클릭율 집계(#466)를 위해 "어떤 경로로 읽혔는지"를 남긴다.
--
-- 읽음 처리 엔드포인트는 이미 푸시(fcm-messages/{fcmMessageId}/read)와
-- 알림함({memberFcmMessageId}/read)으로 갈려 있었지만, 서버가 그 구분을 저장하지 않아
-- is_read만으로는 실제 클릭과 전체 읽음/자동 읽음이 섞여 전환율을 부풀렸다.
--
-- dev/prod는 ddl-auto가 none이라 컬럼이 자동 생성되지 않는다. 앱 배포 전에 먼저 적용해야 한다.
-- (컬럼이 없는 상태로 새 코드가 뜨면 알림 조회/읽음 처리 전체가 깨진다.)
--
-- 이미 읽힌 과거 행은 경로를 알 수 없어 NULL로 남긴다. 이 행들은 readCount에는 잡히지만
-- clickCount에는 빠지므로, 컬럼 추가 이전 발송 건의 클릭율은 0으로 보인다(데이터 부재이지 버그가 아님).

-- 재실행해도 안전하도록 IF NOT EXISTS를 쓴다(fcm_message_add_path_column.sql과 같은 방식).
ALTER TABLE member_fcm_message
    ADD COLUMN IF NOT EXISTS read_source VARCHAR(16) NULL AFTER read_at;

-- 집계 쿼리는 fcm_message_id로 묶은 뒤 행마다 is_read와 read_source를 읽는다.
-- 기존 uk_member_fcm_message_message_member(fcm_message_id, member_id)만으로도 대상 행을
-- 찾을 수는 있지만, 두 컬럼을 읽으려 매 행 테이블을 되짚어야 한다. 한 발송의 수신자가 수만 명인
-- 대량 발송에서 그 랜덤 액세스가 집계 비용의 대부분이라, 인덱스만 읽고 끝나도록 커버링으로 만든다.
-- MySQL 8은 CREATE INDEX IF NOT EXISTS를 지원하지 않으므로, 재실행 시에는 이 문장만 건너뛴다.
CREATE INDEX idx_member_fcm_message_message_read
    ON member_fcm_message (fcm_message_id, is_read, read_source);
