-- 모의수강신청 및 학교 원본 학과/강의 코드 저장용 수동 마이그레이션

ALTER TABLE course_offering
    ADD COLUMN dept_code VARCHAR(255) NULL,
    ADD COLUMN dept_name_raw VARCHAR(255) NULL,
    ADD COLUMN college_code VARCHAR(255) NULL,
    ADD COLUMN college_name_raw VARCHAR(255) NULL,
    ADD COLUMN hy_code VARCHAR(255) NULL,
    ADD COLUMN hy_name_raw VARCHAR(255) NULL,
    ADD COLUMN isu_code VARCHAR(255) NULL,
    ADD COLUMN isu_name_raw VARCHAR(255) NULL,
    ADD COLUMN isu_fld_code VARCHAR(255) NULL,
    ADD COLUMN isu_fld_name_raw VARCHAR(255) NULL,
    ADD COLUMN ssup_type_code VARCHAR(255) NULL,
    ADD COLUMN ssup_type_name_raw VARCHAR(255) NULL,
    ADD COLUMN cnctr_isu_code VARCHAR(255) NULL,
    ADD COLUMN cnctr_isu_name_raw VARCHAR(255) NULL,
    ADD COLUMN english_code VARCHAR(255) NULL,
    ADD COLUMN english_name_raw VARCHAR(255) NULL,
    ADD COLUMN huss_course_yn VARCHAR(10) NULL,
    ADD INDEX idx_course_offering_open_sort (semester_id, hy_code, isu_code, subject_number),
    ADD INDEX idx_course_offering_department (semester_id, dept_code),
    ADD INDEX idx_course_offering_completion (semester_id, isu_code, isu_fld_code);

CREATE TABLE school_department (
    school_department_id BIGINT NOT NULL AUTO_INCREMENT,
    department_code VARCHAR(255) NOT NULL,
    department_name VARCHAR(255) NOT NULL,
    active BIT NOT NULL,
    source_year INT NOT NULL,
    source_term VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NULL,
    modified_at DATETIME(6) NULL,
    PRIMARY KEY (school_department_id),
    CONSTRAINT uk_school_department_code UNIQUE (department_code),
    INDEX idx_school_department_active_name (active, department_name)
);

ALTER TABLE member
    ADD COLUMN school_department_id BIGINT NULL,
    ADD CONSTRAINT fk_member_school_department
        FOREIGN KEY (school_department_id) REFERENCES school_department (school_department_id);

CREATE TABLE mock_watchlist_item (
    mock_watchlist_item_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    course_offering_id BIGINT NOT NULL,
    PRIMARY KEY (mock_watchlist_item_id),
    CONSTRAINT uk_mock_watchlist_member_semester_offering
        UNIQUE (member_id, semester_id, course_offering_id),
    CONSTRAINT fk_mock_watchlist_member FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT fk_mock_watchlist_semester FOREIGN KEY (semester_id) REFERENCES semester (semester_id),
    CONSTRAINT fk_mock_watchlist_offering FOREIGN KEY (course_offering_id) REFERENCES course_offering (course_offering_id),
    INDEX idx_mock_watchlist_lookup (member_id, semester_id)
);

CREATE TABLE mock_enrollment (
    mock_enrollment_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    course_offering_id BIGINT NOT NULL,
    PRIMARY KEY (mock_enrollment_id),
    CONSTRAINT uk_mock_enrollment_member_semester_offering
        UNIQUE (member_id, semester_id, course_offering_id),
    CONSTRAINT fk_mock_enrollment_member FOREIGN KEY (member_id) REFERENCES member (member_id),
    CONSTRAINT fk_mock_enrollment_semester FOREIGN KEY (semester_id) REFERENCES semester (semester_id),
    CONSTRAINT fk_mock_enrollment_offering FOREIGN KEY (course_offering_id) REFERENCES course_offering (course_offering_id),
    INDEX idx_mock_enrollment_lookup (member_id, semester_id)
);
