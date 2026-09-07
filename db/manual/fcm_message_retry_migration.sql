-- 실패한 관리자 알림의 수동 재시도 기능 (실패 대상 한정 재발송)
--
-- 배경: 2026-09-07 관리자 발송(fcmMessageId=13030)에서 3,296건 중 2,147건이 실패했다.
-- 그런데 수신자별 성공/실패 기록이 없어(member_fcm_message는 알림함 읽음 상태만 담는다)
-- 재시도하려면 전원에게 재발송할 수밖에 없었고, 그러면 이미 받은 1,149명이 같은 알림을
-- 두 번 받는다. 그래서 발송 시점에 "끝내 전달하지 못한 회원"을 남겨 두고,
-- 재시도는 그 집합만 대상으로 한다.
--
-- 적용 순서대로 실행한다. 모두 재실행해도 안전하도록 IF NOT EXISTS를 쓴다
-- (MySQL 8.0.29+ 에서 ADD COLUMN IF NOT EXISTS 미지원 버전이면 아래 주석의 수동 확인 후 실행).

-- 1) 재시도 이력 컬럼
--    retry_count   : 관리자가 이 알림을 재시도한 횟수 (재시도 현황 표시용)
--    last_retried_at: 마지막 재시도 시각
ALTER TABLE fcm_message
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_retried_at DATETIME NULL;

-- 2) 전달 실패 회원 기록
--    한 회원이 기기를 여러 대 쓰면 토큰도 여러 개다. 그중 하나라도 성공했다면 그 회원은
--    알림을 받은 것이므로 여기에 남기지 않는다. 즉 "회원의 모든 토큰이 실패한 경우"만 기록한다.
--    재시도는 이 회원들의 '현재' 토큰을 다시 조회해 보내므로, 그 사이 기기를 바꿨어도 전달된다.
CREATE TABLE IF NOT EXISTS fcm_message_failed_target
(
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    fcm_message_id BIGINT   NOT NULL,
    member_id      BIGINT   NOT NULL,
    create_date    DATETIME NULL,
    modified_date  DATETIME NULL,
    PRIMARY KEY (id),
    -- 재시도를 반복해도 같은 회원이 중복 적재되지 않도록 한다.
    UNIQUE KEY uk_fcm_message_failed_target (fcm_message_id, member_id),
    KEY idx_fcm_message_failed_target_message (fcm_message_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- 참고: 이 마이그레이션 이전에 발송된 알림은 실패 회원 기록이 없다. 따라서 과거 건은
-- 재시도 대상 수가 0으로 표시되며 재시도 버튼이 비활성화된다. 의도된 동작이다.
-- 기록 없이 전원 재발송하면 중복 푸시가 나가기 때문이다.
