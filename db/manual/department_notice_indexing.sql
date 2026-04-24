-- 1. 중복된 데이터 확인 및 삭제 (최신 id만 남기고 삭제)
DELETE n1 FROM department_notice n1
INNER JOIN department_notice n2
WHERE n1.id < n2.id
AND n1.department = n2.department
AND n1.url = n2.url;

-- 2. (옵션) 만약 위 쿼리로 부족하다면, 중복 건수 확인을 위해 사용해 보세요.
-- SELECT department, url, COUNT(*)
-- FROM department_notice
-- GROUP BY department, url
-- HAVING COUNT(*) > 1;

-- 1. 학과별 URL 중복 체크 및 조회 최적화 (UNIQUE 제약 추가로 무결성 강화)
CREATE UNIQUE INDEX idx_dept_url ON department_notice (department, url);

-- 2. 학과별 목록 조회 및 정렬(날짜순) 최적화
-- (department, create_date DESC, id DESC) 조합으로 정렬 성능 극대화
CREATE INDEX idx_dept_date_id ON department_notice (department, create_date DESC, id DESC);

-- 3. 제목과 날짜를 이용한 중복 체크 최적화
CREATE INDEX idx_dept_title_date ON department_notice (department, title, create_date);

-- 4. 상태 기반 배치 작업(크롤링/AI 추출) 최적화
CREATE INDEX idx_content_extract_status ON department_notice (content_status, schedule_extract_status);
