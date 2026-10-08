-- 온보딩 실명 입력용 컬럼 추가 (운영 DB가 ddl-auto=update가 아닐 때만 수동 실행)
ALTER TABLE member ADD COLUMN name VARCHAR(20) NULL;
