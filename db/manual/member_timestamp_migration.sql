ALTER TABLE member
    ADD COLUMN IF NOT EXISTS joined_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS profile_modified_at DATETIME NULL,
    ADD COLUMN IF NOT EXISTS last_seen_at DATETIME NULL;

UPDATE member m
LEFT JOIN (
    SELECT candidate.member_id, MIN(candidate.first_seen_at) AS joined_at
    FROM (
        SELECT
            CAST(smli.memberId AS UNSIGNED) AS member_id,
            TIMESTAMP(MIN(sml.date)) AS first_seen_at
        FROM summary_member_log_item smli
        JOIN summary_member_log sml
          ON sml.id = smli.summary_member_log_id
        WHERE smli.memberId REGEXP '^[0-9]+$'
        GROUP BY CAST(smli.memberId AS UNSIGNED)

        UNION ALL

        SELECT
            CAST(l.member_id AS UNSIGNED) AS member_id,
            MIN(l.modified_date) AS first_seen_at
        FROM logging l
        WHERE l.member_id REGEXP '^[0-9]+$'
        GROUP BY CAST(l.member_id AS UNSIGNED)
    ) candidate
    GROUP BY candidate.member_id
) joined_lookup
  ON joined_lookup.member_id = m.id
LEFT JOIN (
    SELECT candidate.member_id, MAX(candidate.last_seen_at) AS last_seen_at
    FROM (
        SELECT
            CAST(smli.memberId AS UNSIGNED) AS member_id,
            TIMESTAMP(MAX(sml.date), '23:59:59') AS last_seen_at
        FROM summary_member_log_item smli
        JOIN summary_member_log sml
          ON sml.id = smli.summary_member_log_id
        WHERE smli.memberId REGEXP '^[0-9]+$'
        GROUP BY CAST(smli.memberId AS UNSIGNED)

        UNION ALL

        SELECT
            CAST(l.member_id AS UNSIGNED) AS member_id,
            MAX(l.modified_date) AS last_seen_at
        FROM logging l
        WHERE l.member_id REGEXP '^[0-9]+$'
          AND l.uri IN ('/api/members', '/api/members/no-dup')
        GROUP BY CAST(l.member_id AS UNSIGNED)
    ) candidate
    GROUP BY candidate.member_id
) seen_lookup
  ON seen_lookup.member_id = m.id
SET m.joined_at = COALESCE(m.joined_at, joined_lookup.joined_at, NOW()),
    m.profile_modified_at = COALESCE(m.profile_modified_at, joined_lookup.joined_at, NOW()),
    m.last_seen_at = COALESCE(m.last_seen_at, seen_lookup.last_seen_at);

ALTER TABLE member
    MODIFY COLUMN joined_at DATETIME NOT NULL,
    MODIFY COLUMN profile_modified_at DATETIME NOT NULL;
