-- send_status 컬럼(5f8e2483, 2026-03-18) 도입 이전에 생성된 fcm_message 행은
-- 기본값 PENDING이 소급 backfill된 것으로, 실제 발송 대기 상태가 아니다.
--
-- :CUTOFF 는 이번 배포(코드 배포 시각)로 고정한다. 향후 유실 보정 스케줄러가
-- 도입되면 같은 값을 fcm.recovery.not-before 하한과 맞춰야, 마이그레이션과
-- 스케줄러가 동일한 경계를 공유한다.
--
-- create_date가 NULL인 행(JPA 저장 경로를 거치지 않은 레거시/시딩 데이터, #431 참고)도
-- 함께 정리 대상에 포함한다. NULL 비교는 SQL에서 항상 UNKNOWN이라 누락되기 쉽다.

UPDATE fcm_message
SET send_status = 'ABANDONED'
WHERE send_status = 'PENDING'
  AND (create_date IS NULL OR create_date < :CUTOFF);
