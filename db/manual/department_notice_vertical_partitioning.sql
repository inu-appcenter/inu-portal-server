-- 1. 새로운 본문 테이블 생성
CREATE TABLE department_notice_content (
    id BIGINT NOT NULL,
    content_html LONGTEXT,
    content_text LONGTEXT,
    ocr_text LONGTEXT,
    attachment_text LONGTEXT,
    merged_text LONGTEXT,
    inline_image_urls_json LONGTEXT,
    attachment_meta_json LONGTEXT,
    schedule_extract_response_json LONGTEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_department_notice_content_id FOREIGN KEY (id) REFERENCES department_notice (id) ON DELETE CASCADE
);

-- 2. 기존 데이터 마이그레이션
INSERT INTO department_notice_content (
    id, content_html, content_text, ocr_text, attachment_text, merged_text,
    inline_image_urls_json, attachment_meta_json, schedule_extract_response_json
)
SELECT 
    id, content_html, content_text, ocr_text, attachment_text, merged_text,
    inline_image_urls_json, attachment_meta_json, schedule_extract_response_json
FROM department_notice;

-- 3. 기존 테이블에서 본문 컬럼 삭제
ALTER TABLE department_notice DROP COLUMN content_html;
ALTER TABLE department_notice DROP COLUMN content_text;
ALTER TABLE department_notice DROP COLUMN ocr_text;
ALTER TABLE department_notice DROP COLUMN attachment_text;
ALTER TABLE department_notice DROP COLUMN merged_text;
ALTER TABLE department_notice DROP COLUMN inline_image_urls_json;
ALTER TABLE department_notice DROP COLUMN attachment_meta_json;
ALTER TABLE department_notice DROP COLUMN schedule_extract_response_json;
