-- =====================================================================
-- course_offering 과거(편람 엑셀) 행 *_code 백필   (MySQL 8 / inu_portal, inu_portal_dev)
--
-- [문제]
--   과거 강의 적재기(POST /api/course-offerings/legacy, LegacyCourseExcelImporter)가
--   새 행에는 *_code 를 비워서 넣고, 이미 있는 행을 갱신할 때도 null 로 덮어썼다.
--   개설강의 필터(학과·단과대·학년·이수구분·이수영역 …)는 *_code 로 거르므로, 코드가 빈 행은
--   DB 에 있어도 시간표 과목 검색 필터에서 빠진다. (courseOffering_enum_migration.sql PHASE 2 로
--   한 번 채웠지만, 이후 적재가 다시 null 로 되돌렸다.)
--
-- [수정]
--   적재기는 LegacyCourseCodeResolver 로 이름 → 코드를 채우고, 모르는 이름이면 기존 코드를 유지한다.
--   이 스크립트는 이미 비어 버린 행을 같은 매핑으로 채운다.
--
-- [실행]
--   - 새 적재기 배포 전후 아무 때나, 트래픽 중 실행 가능. 멱등(… IS NULL 인 행만 갱신).
--   - STEP 0 으로 현황을 본 뒤 STEP 1 의 UPDATE 를 실행하고 STEP 2 로 검증한다.
--   - 매핑은 courseOffering_enum_migration.sql PHASE 2 와 동일(편람 실제 값 전수).
--     매핑을 바꾸면 LegacyCourseCodeResolver 도 같이 바꾼다.
--
-- [NULL 로 남는 값(정상)]
--   dept_code : 국제개발협력연계전공(enum 코드 없음), 뷰티산업연계전공, MICE,스포츠및관광연계전공,
--               인공지능·창업연계전공, 인공지능소프트웨어연계전공, 동북아국제통상물류학부, dept_name_raw IS NULL
--   ssup_type_code : b-Learning, f-Learning, 진로설계세미나, 사회봉사(4), 원어(영어), 현장형
--   cnctr_isu_code : 집중B(9~15주)
--   (위 값들은 학교 API 로 들어온 행에도 코드가 없어 매핑 근거가 없다.)
--   isu_code  : 편람 옛 이수구분(전공필수/전공선택/공통필수/교양필수 등)
--   english_code : 편람 엑셀에 영어강의 구분 이름이 없음(english_yn 만 있음) — 이 스크립트 대상 아님
-- =====================================================================


-- ===================== STEP 0 : 현황 =====================

SELECT
    SUM(dept_code IS NULL AND dept_name_raw IS NOT NULL)            AS dept_null,
    SUM(college_code IS NULL AND college_name_raw IS NOT NULL)      AS college_null,
    SUM(hy_code IS NULL AND hy_name_raw IS NOT NULL)                AS hy_null,
    SUM(isu_code IS NULL AND isu_name_raw IS NOT NULL)              AS isu_null,
    SUM(isu_fld_code IS NULL AND isu_fld_name_raw IS NOT NULL)      AS isu_fld_null,
    SUM(ssup_type_code IS NULL AND ssup_type_name_raw IS NOT NULL)  AS ssup_type_null,
    SUM(cnctr_isu_code IS NULL AND cnctr_isu_name_raw IS NOT NULL)  AS cnctr_isu_null
FROM course_offering;


-- ===================== STEP 1 : 백필 =====================

-- 2-1
UPDATE course_offering
SET college_code = CASE college_name_raw
    WHEN '인문대학'              THEN 'A000'
    WHEN '자연과학대학'          THEN 'B000'
    WHEN '사회과학대학'          THEN 'C000'
    WHEN '공과대학'              THEN 'E000'
    WHEN '정보기술대학'          THEN 'I000'
    WHEN '경영대학'              THEN 'J000'
    WHEN '교양'                  THEN 'X000'
    WHEN '일선'                  THEN 'W000'
    WHEN '교직'                  THEN 'Y000'
    WHEN '군사학'                THEN 'Z000'
    WHEN '기타'                  THEN 'V000'
    WHEN '글로벌정경대학'        THEN '0000689'
    WHEN '예술체육대학'          THEN '0000190'
    WHEN '사범대학'              THEN '0000063'
    WHEN '도시과학대학'          THEN '0000033'
    WHEN '생명과학기술대학'      THEN '0000182'
    WHEN '융합자유전공대학'      THEN '0000837'
    WHEN '단과대구분없음'        THEN '0000465'
    WHEN '단과대구분없음(법학)'  THEN '0000706'
END
WHERE college_code IS NULL AND college_name_raw IS NOT NULL;

-- 2-2
UPDATE course_offering
SET hy_code = CASE hy_name_raw
    WHEN '전학년' THEN '0'
    WHEN '1'      THEN '1'
    WHEN '2'      THEN '2'
    WHEN '3'      THEN '3'
    WHEN '4'      THEN '4'
END
WHERE hy_code IS NULL AND hy_name_raw IS NOT NULL;

-- 2-3
UPDATE course_offering
SET isu_code = CASE isu_name_raw
    WHEN '기초교양'  THEN '11'
    WHEN '핵심교양'  THEN '21'
    WHEN '심화교양'  THEN '23'
    WHEN '전공기초'  THEN '25'
    WHEN '전공핵심'  THEN '31'
    WHEN '전공심화'  THEN '41'
    WHEN '교직'      THEN '50'
    WHEN '군사학'    THEN '70'
    WHEN '일반선택'  THEN '80'
END
WHERE isu_code IS NULL AND isu_name_raw IS NOT NULL;

-- 2-5
UPDATE course_offering
SET cnctr_isu_code = CASE cnctr_isu_name_raw
    WHEN '일반(1~15주)'  THEN '0'
    WHEN '집중A(1~8주)'  THEN '1'
    WHEN '집중C(1~12주)' THEN '3'
END
WHERE cnctr_isu_code IS NULL AND cnctr_isu_name_raw IS NOT NULL;

-- 2-6
UPDATE course_offering
SET isu_fld_code = CASE isu_fld_name_raw
    WHEN '학문의기초'      THEN '161'
    WHEN '기초과학ㆍ공학'  THEN '162'
    WHEN '기초과학·공학'   THEN '162'
    WHEN '(핵심)INU세미나' THEN '171'
    WHEN '(핵심)인문'      THEN '172'
    WHEN '(핵심)사회'      THEN '173'
    WHEN '(핵심)과학기술'  THEN '174'
    WHEN '(핵심)예술체육'  THEN '175'
    WHEN '(핵심)외국어'    THEN '176'
    WHEN '인문'            THEN '182'
    WHEN '사회'            THEN '183'
    WHEN '과학기술'        THEN '184'
    WHEN '예술체육'        THEN '185'
    WHEN '외국어'          THEN '186'
    WHEN '전공기초'        THEN '31'
    WHEN '전공핵심'        THEN '34'
    WHEN '전공심화'        THEN '35'
    WHEN '교직'            THEN '51'
    WHEN '군사학'          THEN '71'
    WHEN '일반선택'        THEN '81'
END
WHERE isu_fld_code IS NULL AND isu_fld_name_raw IS NOT NULL;

-- 2-7
UPDATE course_offering
SET ssup_type_code = CASE ssup_type_name_raw
    WHEN '강의(이론)'             THEN '1'
    WHEN '실험실습'               THEN '2'
    WHEN '체육실기'               THEN '3'
    WHEN '미술실기'               THEN '4'
    WHEN '이론실험실습'           THEN '5'
    WHEN '열린사이버대학(OCU)'    THEN '7'
    WHEN 'e-Learning'             THEN '8'
    WHEN '담장너머~,사회봉사(1)'  THEN '11'
    WHEN '사회봉사(2)'            THEN '12'
    WHEN '사회봉사(3)'            THEN '13'
    WHEN '자기설계세미나'         THEN '17'
    WHEN '이론(어학)'             THEN '20'
    WHEN 'RISE(시간표 있음)'      THEN '21'
    WHEN 'RISE(시간표 없음)'      THEN '22'
    WHEN '예술체육실기'           THEN '23'
    WHEN '온라인혼합형강좌'       THEN '24'
    WHEN 'K-MOOC'                 THEN '25'
    WHEN 'e-Learning(HUSS)'       THEN '26'
    WHEN '온라인혼합형강좌(HUSS)' THEN '27'
    WHEN '현장형(HUSS)'           THEN '28'
END
WHERE ssup_type_code IS NULL AND ssup_type_name_raw IS NOT NULL;

-- 2-8  dept_code  (실제 dept_name_raw DISTINCT 104개 전수)
UPDATE course_offering
SET dept_code = CASE dept_name_raw
    WHEN '국어국문학과'              THEN 'AIA1'
    WHEN '영어영문학과'              THEN 'AIB1'
    WHEN '중어중국학과'              THEN 'AID1'
    WHEN '독어독문학과'              THEN 'AIE1'
    WHEN '불어불문학과'              THEN 'AIF1'
    WHEN '일본지역문화학과'          THEN '0000793'
    WHEN '일어일문학과'              THEN '0000793'
    WHEN '수학과'                    THEN 'BKA1'
    WHEN '물리학과'                  THEN 'BKB1'
    WHEN '화학과'                    THEN 'BKC1'
    WHEN '패션산업학과'              THEN 'BLB1'
    WHEN '해양학과'                  THEN '0000189'
    WHEN '사회복지학과'              THEN '0000144'
    WHEN '미디어커뮤니케이션학과'    THEN '0000794'
    WHEN '신문방송학과'              THEN '0000794'
    WHEN '문헌정보학과'              THEN '0000053'
    WHEN '창의인재개발학과'          THEN '0000054'
    WHEN '행정학과'                  THEN '0000698'
    WHEN '정치외교학과'              THEN '0000699'
    WHEN '경제학과'                  THEN '0000700'
    WHEN '경제학과(야)'              THEN '0000701'
    WHEN '무역학부'                  THEN '0000913'
    WHEN 'Global Trade & Service학부' THEN '0000913'
    WHEN '무역학부(야)'              THEN '0000703'
    WHEN '소비자학과'                THEN '0000704'
    WHEN '소비자ㆍ아동학과'          THEN '0000704'
    WHEN '에너지화학공학과'          THEN '0000055'
    WHEN '전기공학과'                THEN 'EPB1'
    WHEN '전자공학부'                THEN '0000813'
    WHEN '전자공학과'                THEN 'EPC1'
    WHEN '전자공학과(야)'            THEN 'EPC1'
    WHEN '전자공학전공'              THEN '0000828'
    WHEN '산업경영공학과'            THEN 'EPG1'
    WHEN '산업경영공학과(야)'        THEN 'EPG1'
    WHEN '신소재공학과'              THEN '0000076'
    WHEN '기계공학과'                THEN '0000459'
    WHEN '기계공학과(야)'            THEN '0000459'
    WHEN '메카트로닉스공학과'        THEN '0000814'
    WHEN '바이오-로봇시스템공학과'   THEN '0000814'
    WHEN '안전공학과'                THEN '0000075'
    WHEN '컴퓨터공학부'              THEN '0000077'
    WHEN '컴퓨터공학부(야)'          THEN '0000077'
    WHEN '정보통신공학과'            THEN 'IAB1'
    WHEN '임베디드시스템공학과'      THEN '0000042'
    WHEN '경영학부'                  THEN 'JA01'
    WHEN '데이터과학과'              THEN '0000812'
    WHEN '세무회계학과'              THEN '0000057'
    WHEN '조형예술학부'              THEN '0000192'
    WHEN '한국화전공'                THEN '0000193'
    WHEN '서양화전공'                THEN '0000194'
    WHEN '디자인학부'                THEN '0000195'
    WHEN '공연예술학과'              THEN '0000196'
    WHEN '스포츠과학부'              THEN '0000815'
    WHEN '체육학부'                  THEN '0000815'
    WHEN '운동건강학부'              THEN '0000191'
    WHEN '국어교육과'                THEN '0000064'
    WHEN '영어교육과'                THEN '0000065'
    WHEN '일어교육과'                THEN '0000066'
    WHEN '수학교육과'                THEN '0000067'
    WHEN '체육교육과'                THEN '0000068'
    WHEN '유아교육과'                THEN '0000069'
    WHEN '역사교육과'                THEN '0000070'
    WHEN '윤리교육과'                THEN '0000071'
    WHEN '도시행정학과'              THEN '0000073'
    WHEN '건설환경공학전공'          THEN '0000156'
    WHEN '건설환경공학부'            THEN '0000156'
    WHEN '환경공학전공'              THEN '0000157'
    WHEN '도시환경공학부'            THEN '0000034'
    WHEN '도시공학과'                THEN '0000463'
    WHEN '도시건축학부'              THEN '0000038'
    WHEN '건축공학전공'              THEN '0000160'
    WHEN '도시건축학전공'            THEN '0000464'
    WHEN '생명과학부'                THEN '0000183'
    WHEN '생명과학전공'              THEN '0000184'
    WHEN '분자의생명전공'            THEN '0000185'
    WHEN '생명공학부'                THEN '0000186'
    WHEN '생명공학전공'              THEN '0000187'
    WHEN '나노바이오공학전공'        THEN '0000833'
    WHEN '나노바이오전공'            THEN '0000833'
    WHEN '자유전공학부'              THEN '0000838'
    WHEN '법학부'                    THEN '0000707'
    WHEN 'IBE전공'                   THEN '0000832'
    WHEN '한국통상전공'              THEN '0000832'
    WHEN '스마트물류공학전공'        THEN '0000818'
    WHEN '동북아국제통상전공'        THEN '0000817'
    WHEN '동북아통상전공'            THEN '0000817'
    WHEN '동북아국제통상학부'        THEN '0000817'
    WHEN '반도체융합전공'            THEN '0000829'
    WHEN '광전자공학전공(연계)'      THEN 'VAB1'
    WHEN '물류학전공(연계)'          THEN 'VAC1'
    WHEN '미래교육디자인연계전공'    THEN '0000849'
    WHEN '미래자동차연계전공'        THEN '0000789'
    WHEN '소셜데이터사이언스연계전공' THEN '0000678'
    WHEN '인문문화예술기획연계전공'   THEN '0000677'
    WHEN '지능형로봇시스템연계전공'   THEN '0000912'
    WHEN '지능로봇연계전공'          THEN '0000912'
    WHEN '창의적디자인연계전공'      THEN '0000616'
    WHEN '교양'                      THEN 'XAA0'
    WHEN '일선'                      THEN 'WAA0'
    WHEN '교직'                      THEN 'YAA0'
    WHEN '군사학'                    THEN 'ZAA0'
    WHEN 'HUSS(타대학)'              THEN 'VEA1'
    WHEN 'HUSS포용사회이니셔티브학부' THEN 'VE00'
END
WHERE dept_code IS NULL AND dept_name_raw IS NOT NULL;


-- ===================== STEP 2 : 검증 =====================

-- STEP 0 과 같은 쿼리. [NULL 로 남는 값] 외에는 0 이어야 한다.
SELECT
    SUM(dept_code IS NULL AND dept_name_raw IS NOT NULL)            AS dept_null,
    SUM(college_code IS NULL AND college_name_raw IS NOT NULL)      AS college_null,
    SUM(hy_code IS NULL AND hy_name_raw IS NOT NULL)                AS hy_null,
    SUM(isu_code IS NULL AND isu_name_raw IS NOT NULL)              AS isu_null,
    SUM(isu_fld_code IS NULL AND isu_fld_name_raw IS NOT NULL)      AS isu_fld_null,
    SUM(ssup_type_code IS NULL AND ssup_type_name_raw IS NOT NULL)  AS ssup_type_null,
    SUM(cnctr_isu_code IS NULL AND cnctr_isu_name_raw IS NOT NULL)  AS cnctr_isu_null
FROM course_offering;

-- 남은 이름 확인 (매핑 누락 여부)
SELECT dept_name_raw, COUNT(*) FROM course_offering
WHERE dept_code IS NULL AND dept_name_raw IS NOT NULL GROUP BY dept_name_raw;

SELECT isu_name_raw, COUNT(*) FROM course_offering
WHERE isu_code IS NULL AND isu_name_raw IS NOT NULL GROUP BY isu_name_raw;

-- 제보 사례: 2024-2학기 도시건축학부(0000038) 과목이 학과 필터에 잡히는지
SELECT co.subject_number, co.dept_name_raw, co.dept_code, co.isu_code, co.hy_code
FROM course_offering co
JOIN semester s ON s.semester_id = co.semester_id
WHERE s.academic_year = 2024 AND s.term = 'SECOND' AND co.dept_name_raw = '도시건축학부';
