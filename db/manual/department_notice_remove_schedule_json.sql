-- department_notice_content 테이블에서 불필요한 AI 응답 원본 JSON 컬럼 삭제
ALTER TABLE department_notice_content DROP COLUMN schedule_extract_response_json;
