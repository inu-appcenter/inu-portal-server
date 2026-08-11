"""
insert_to_mysql.py
------------------
SQLite course.db 데이터를 실제 MySQL/MariaDB course 테이블에 upsert 합니다.

실제 DB 스키마 기준:
  - create_date   DATETIME(6)   (BaseTimeEntity)
  - modified_date DATETIME(6)   (BaseTimeEntity)
  - active        BIT(1)
  - department    ENUM(...)
  - college       ENUM(...)

동작 방식:
  1. 엑셀 기반 레코드를 UPSERT (ON DUPLICATE KEY UPDATE)
     - (title, department) 충돌 시: active / course_code / credit /
       completion_division / target_grade 갱신
     - content, english_title, target_term 은 기존 값 보존
  2. 2025년에 개설되지 않은 강의 → active = 0
  3. 2025년에 개설된 강의     → active = 1
"""

import os
import sqlite3
from datetime import datetime, timezone
from dotenv import load_dotenv
import pymysql
from pymysql.cursors import DictCursor

# ──────────────────────────────────────────────────────────────────────────────
# 1. 환경변수 로드
# ──────────────────────────────────────────────────────────────────────────────

load_dotenv()

DB_HOST = os.environ["DB_HOST"]
DB_PORT = int(os.environ.get("DB_PORT", 3306))
DB_NAME = os.environ["DB_NAME"]
DB_USER = os.environ["DB_USER"]
DB_PASS = os.environ["DB_PASSWORD"]

# ──────────────────────────────────────────────────────────────────────────────
# 2. SQLite에서 데이터 읽기
# ──────────────────────────────────────────────────────────────────────────────

sqlite_conn = sqlite3.connect("course.db")
sqlite_conn.row_factory = sqlite3.Row
rows = sqlite_conn.execute("SELECT * FROM course ORDER BY course_id").fetchall()
sqlite_conn.close()

print(f"✅ SQLite에서 {len(rows):,}개 레코드 로드 완료")
active_cnt   = sum(1 for r in rows if r["active"])
inactive_cnt = len(rows) - active_cnt
print(f"   active=1(현역): {active_cnt:,}개 / active=0(폐지): {inactive_cnt:,}개")

# ──────────────────────────────────────────────────────────────────────────────
# 3. MySQL 연결
# ──────────────────────────────────────────────────────────────────────────────

conn = pymysql.connect(
    host=DB_HOST, port=DB_PORT, db=DB_NAME,
    user=DB_USER, password=DB_PASS,
    charset="utf8mb4", cursorclass=DictCursor,
    autocommit=False,
)
print(f"✅ MySQL 연결 성공: {DB_USER}@{DB_HOST}:{DB_PORT}/{DB_NAME}")

# ──────────────────────────────────────────────────────────────────────────────
# 4. UPSERT 쿼리
#    - (title, department) 충돌 → active/code/credit/division/grade 갱신
#    - content, english_title, target_term 은 기존 값 유지
# ──────────────────────────────────────────────────────────────────────────────

UPSERT_SQL = """
INSERT INTO course
    (course_code, title, english_title, department, college,
     target_grade, target_term, completion_division, credit, content,
     active, create_date, modified_date)
VALUES
    (%(course_code)s, %(title)s, %(english_title)s, %(department)s, %(college)s,
     %(target_grade)s, %(target_term)s, %(completion_division)s, %(credit)s, %(content)s,
     %(active)s, %(now)s, %(now)s)
ON DUPLICATE KEY UPDATE
    course_code         = IF(VALUES(course_code) IS NOT NULL, VALUES(course_code), course_code),
    active              = VALUES(active),
    completion_division = IF(VALUES(completion_division) != 'UNKNOWN', VALUES(completion_division), completion_division),
    target_grade        = IF(VALUES(target_grade) != 'UNKNOWN', VALUES(target_grade), target_grade),
    credit              = IF(VALUES(credit) IS NOT NULL, VALUES(credit), credit),
    modified_date       = VALUES(modified_date)
"""

# ──────────────────────────────────────────────────────────────────────────────
# 5. 데이터 준비
# ──────────────────────────────────────────────────────────────────────────────

now_str = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S.%f")

data = []
for row in rows:
    data.append({
        "course_code":         row["course_code"],
        "title":               row["title"],
        "english_title":       row["english_title"],   # None → DB 기존값 유지
        "department":          row["department"],
        "college":             row["college"],
        "target_grade":        row["target_grade"] or "UNKNOWN",
        "target_term":         row["target_term"]  or "UNKNOWN",
        "completion_division": row["completion_division"] or "UNKNOWN",
        "credit":              row["credit"],
        "content":             row["content"],          # None → DB 기존값 유지
        "active":              1 if row["active"] else 0,
        "now":                 now_str,
    })

# ──────────────────────────────────────────────────────────────────────────────
# 6. 배치 UPSERT (500행 단위)
# ──────────────────────────────────────────────────────────────────────────────

BATCH = 500
total = len(data)
processed = 0

try:
    with conn.cursor() as cur:
        for i in range(0, total, BATCH):
            batch = data[i : i + BATCH]
            cur.executemany(UPSERT_SQL, batch)
            conn.commit()
            processed += len(batch)
            pct = processed / total * 100
            print(f"   진행: {processed:,} / {total:,} ({pct:.1f}%)", end="\r")

    print(f"\n✅ UPSERT 완료: {processed:,}개 처리")

except Exception as e:
    conn.rollback()
    print(f"\n❌ 오류 발생 — 롤백")
    raise e

# ──────────────────────────────────────────────────────────────────────────────
# 7. 결과 검증
# ──────────────────────────────────────────────────────────────────────────────

with conn.cursor() as cur:
    cur.execute("SELECT COUNT(*) as total, SUM(active) as act FROM course")
    r = conn.cursor().execute
    cur.execute("SELECT COUNT(*) as total, SUM(active=1) as act FROM course")
    res = cur.fetchone()
    total_db  = res["total"]
    active_db = res["act"] or 0

    print(f"\n📊 DB 최종 상태:")
    print(f"   전체 강의: {total_db:,}개")
    print(f"   active=1 : {active_db:,}개")
    print(f"   active=0 : {total_db - active_db:,}개")

    print("\n─── active=0 샘플 5개 (폐지 강의) ─────────────────────────")
    cur.execute("""
        SELECT course_id, course_code, title, department, active
        FROM course WHERE active = 0 LIMIT 5
    """)
    for row in cur.fetchall():
        print(f"  {row}")

conn.close()
print("\n🎉 완료!")
