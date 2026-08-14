"""
inspect_db.py
-------------
.env의 연결 정보로 MySQL에 접속해 DB 구조를 출력합니다.
"""

import os
from dotenv import load_dotenv
import pymysql
from pymysql.cursors import DictCursor

load_dotenv()

DB_HOST = os.environ["DB_HOST"]
DB_PORT = int(os.environ.get("DB_PORT", 3306))
DB_NAME = os.environ["DB_NAME"]
DB_USER = os.environ["DB_USER"]
DB_PASS = os.environ["DB_PASSWORD"]

conn = pymysql.connect(
    host=DB_HOST, port=DB_PORT, db=DB_NAME,
    user=DB_USER, password=DB_PASS,
    charset="utf8mb4", cursorclass=DictCursor,
)

print(f"✅ 연결 성공: {DB_USER}@{DB_HOST}:{DB_PORT}/{DB_NAME}\n")

with conn.cursor() as cur:
    # 테이블 목록
    cur.execute("SHOW TABLES")
    tables = [list(row.values())[0] for row in cur.fetchall()]
    print(f"📋 테이블 목록 ({len(tables)}개)")
    for t in tables:
        print(f"  - {t}")

    # course 테이블이 있으면 상세 확인
    if "course" in tables:
        print("\n─── course 테이블 컬럼 ──────────────────────────────────")
        cur.execute("SHOW FULL COLUMNS FROM course")
        for col in cur.fetchall():
            nullable = "NULL" if col["Null"] == "YES" else "NOT NULL"
            key = f" [{col['Key']}]" if col["Key"] else ""
            print(f"  {col['Field']:<25} {col['Type']:<25} {nullable}{key}")

        print("\n─── course 테이블 인덱스 ────────────────────────────────")
        cur.execute("SHOW INDEX FROM course")
        for idx in cur.fetchall():
            print(f"  {idx['Key_name']:<35} col={idx['Column_name']}  unique={not idx['Non_unique']}")

        print("\n─── 행 수 & active 분포 ─────────────────────────────────")
        cur.execute("SELECT COUNT(*) as total, SUM(active) as active_cnt FROM course")
        row = cur.fetchone()
        print(f"  전체: {row['total']:,}개  /  active=1: {int(row['active_cnt'] or 0):,}개  /  active=0: {int(row['total']) - int(row['active_cnt'] or 0):,}개")

        print("\n─── 샘플 5행 ────────────────────────────────────────────")
        cur.execute("SELECT course_id, course_code, title, department, college, completion_division, credit, active FROM course LIMIT 5")
        for r in cur.fetchall():
            print(f"  {r}")

    else:
        print("\nℹ️  course 테이블이 아직 없습니다. insert_to_mysql.py를 먼저 실행하세요.")

conn.close()
