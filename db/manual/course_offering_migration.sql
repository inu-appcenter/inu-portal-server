  DELETE FROM course_meeting;

  DELETE FROM timetable_item
  WHERE course_offering_id IS NOT NULL;

  DELETE FROM course_offering;

  ALTER TABLE course_offering
      DROP COLUMN method,
      DROP COLUMN target_department,
      ADD COLUMN cnctr_isu_name varchar(255) NULL,
      ADD COLUMN dept_name varchar(255) NULL,
      ADD COLUMN college_name varchar(255) NULL,
      ADD COLUMN isu_fld_name varchar(255) NULL,
      ADD COLUMN isu_name varchar(255) NULL,
      ADD COLUMN ssup_type_name varchar(255) NULL,
      ADD COLUMN hy_name varchar(255) NULL,
      ADD COLUMN english_name varchar(255) NULL,
      ADD COLUMN credit int NULL;
