-- Daily Brief 설정을 보유하지 않은 모든 회원에 대해 기본 알림(ON) 설정 일괄 생성
INSERT INTO daily_brief_setting (
    member_id,
    timetable_alert_enabled,
    timetable_pre_alert_enabled,
    timetable_pre_alert_minutes,
    timetable_daily_brief_enabled,
    timetable_daily_brief_time,
    schedule_alert_enabled,
    schedule_daily_brief_time,
    schedule_scope
)
SELECT 
    m.id,
    TRUE,
    TRUE,
    10,
    TRUE,
    '08:00',
    TRUE,
    '08:30',
    'ALL'
FROM member m
LEFT JOIN daily_brief_setting dbs ON m.id = dbs.member_id
WHERE dbs.member_id IS NULL;
