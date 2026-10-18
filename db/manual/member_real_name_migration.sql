-- 회원 실명과 포털 실명 동의 기록. (시간 맞추기 8단계)
-- 시간 맞추기 표시 이름(displayName)은 실명이 있으면 실명, 없으면 닉네임이다.
--
-- dev/prod는 ddl-auto가 none이라 칸·테이블이 자동 생성되지 않는다. 서버 배포 전에 먼저 적용해야 한다.
-- MySQL 8 기준. 한 번만 실행한다.
ALTER TABLE member
    ADD COLUMN real_name        VARCHAR(30) NULL,
    ADD COLUMN real_name_source VARCHAR(20) NULL;

-- 포털에서 실명을 가져오는 데 대한 동의·철회 기록. 지우지 않고 쌓는다.
CREATE TABLE IF NOT EXISTS member_name_consent (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    member_id     BIGINT       NOT NULL,
    -- AGREE / WITHDRAW
    action        VARCHAR(10)  NOT NULL,
    -- 동의한 약관 버전. 철회면 NULL
    terms_version VARCHAR(50)  NULL,
    recorded_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_member_name_consent_member (member_id)
);
