-- 1. 임시 DATE 컬럼 생성
ALTER TABLE department_notice ADD COLUMN create_date_new DATE;

-- 2. 기존 문자열 데이터를 DATE 타입으로 변환하여 복사 (yyyy.mm.dd -> DATE)
-- 형식이 정확히 yyyy.mm.dd이므로 STR_TO_DATE를 사용합니다.
UPDATE department_notice 
SET create_date_new = STR_TO_DATE(create_date, '%Y.%m.%d')
WHERE create_date IS NOT NULL AND create_date != '';

-- 3. 기존 컬럼 삭제
ALTER TABLE department_notice DROP COLUMN create_date;

-- 4. 임시 컬럼 이름을 원래 이름으로 변경 및 NOT NULL 설정
ALTER TABLE department_notice CHANGE COLUMN create_date_new create_date DATE NOT NULL;
