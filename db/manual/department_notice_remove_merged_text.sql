-- department_notice_content 테이블에서 중복된 merged_text 컬럼 삭제
ALTER TABLE department_notice_content DROP COLUMN merged_text;
