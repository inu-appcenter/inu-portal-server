-- 성적계산기에서 저장 후 재조회 시 전공기초/전공핵심 등 이수구분이 사라지는 문제 수정.
-- grade_record 에는 이수구분을 저장할 컬럼이 아예 없어서(과목명/학점/성적/전공여부/재수강여부만 저장)
-- 저장 시점에 그 정보가 유실되고 있었다. isu_name_raw/isu_fld_name_raw 컬럼 컨벤션(VARCHAR(255))을 따른다.
ALTER TABLE grade_record
    ADD COLUMN isu_name VARCHAR(255) NULL,
    ADD COLUMN isu_fld_name VARCHAR(255) NULL;
