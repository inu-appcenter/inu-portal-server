-- 시간표 Live Activity를 앱이 꺼져 있어도 시작할 수 있도록(iOS ActivityKit push-to-start)
-- 기기별 push-to-start 토큰을 FCM 토큰 행에 함께 저장한다.
--
-- FCM으로 Live Activity를 보낼 때는 "어느 기기인지"(FCM 등록 토큰)와 "어느 ActivityKit 토큰인지"
-- (apns.live_activity_token)를 한 메시지에 같이 실어야 하므로, 별도 테이블 대신 같은 행에 둔다.
--
-- dev/prod는 ddl-auto가 none이라 컬럼이 자동 생성되지 않는다. 서버 배포 전에 먼저 적용해야 한다.
ALTER TABLE fcm_token
    ADD COLUMN IF NOT EXISTS live_activity_start_token VARCHAR(512) NULL AFTER device_type;
