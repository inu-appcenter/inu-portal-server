-- post 테이블의 create_date 컬럼 타입을 DATE에서 DATETIME으로 변경
-- 기존 DATE 데이터(YYYY-MM-DD)는 자동으로 DATETIME(YYYY-MM-DD 00:00:00)으로 변환됩니다.

ALTER TABLE post MODIFY COLUMN create_date DATETIME;
