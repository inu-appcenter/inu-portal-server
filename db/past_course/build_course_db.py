"""
build_course_db.py
------------------
강의계획서 엑셀 파일들을 파싱하여 Course JPA 스키마에 맞는 SQLite DB를 생성합니다.

Unique constraints:
  - uk_course_title_department : (title, department)
  - uk_course_course_code      : course_code

active 필드:
  - 2025년(최신 연도)에 한 번이라도 개설된 강의 → true
  - 이전 연도에만 개설된 강의 → false
"""

import glob
import sqlite3
import warnings
from datetime import datetime, timezone

import pandas as pd

warnings.filterwarnings("ignore")

# ──────────────────────────────────────────────────────────────────────────────
# 1. Enum 매핑 정의
# ──────────────────────────────────────────────────────────────────────────────

# CompletionDivision: description / shortName → enum name
COMPLETION_DIVISION_MAP: dict[str, str] = {
    "기초교양": "BASIC_GENERAL",   "기교": "BASIC_GENERAL",
    "핵심교양": "CORE_GENERAL",    "핵교": "CORE_GENERAL",
    "심화교양": "DEEPEN_GENERAL",  "심교": "DEEPEN_GENERAL",
    "전공기초": "BASIC_MAJOR",     "전기": "BASIC_MAJOR",
    "전공핵심": "CORE_MAJOR",      "전핵": "CORE_MAJOR",
    "전공심화": "DEEPEN_MAJOR",    "전심": "DEEPEN_MAJOR",
    "교직":   "EDUCATION",         "군사학": "MILITARY",
    "일반선택": "SELECT_COMMON",   "일선": "SELECT_COMMON",
    # 현재 사용하지 않는 값 → UNKNOWN 처리
    "기초과학": "UNKNOWN",
    "교양필수": "UNKNOWN",
    "교양선택": "UNKNOWN",
    "전공필수": "UNKNOWN",
    "전공선택": "UNKNOWN",
}

# TargetGrade: 학년 문자열 → enum name
GRADE_MAP: dict[str, str] = {
    "전학년": "COMMON",
    "1": "FIRST",   "1학년": "FIRST",
    "2": "SECOND",  "2학년": "SECOND",
    "3": "THIRD",   "3학년": "THIRD",
    "4": "FOURTH",  "4학년": "FOURTH",
}

# Department: 한국어 학과명 → (enum name, College enum name)
# College는 Department enum 정의에서 추출
DEPARTMENT_MAP: dict[str, tuple[str, str]] = {
    # 인문대학
    "국어국문학과":         ("KOREAN",           "COLLEGE_OF_HUMANITIES"),
    "영어영문학과":         ("ENGLISH",          "COLLEGE_OF_HUMANITIES"),
    "독어독문학과":         ("GERMAN",           "COLLEGE_OF_HUMANITIES"),
    "불어불문학과":         ("FRENCH",           "COLLEGE_OF_HUMANITIES"),
    "일본지역문화학과":     ("JAPANESE",         "COLLEGE_OF_HUMANITIES"),
    "중어중국학과":         ("CHINESE",          "COLLEGE_OF_HUMANITIES"),

    # 자연과학대학
    "수학과":               ("MATHEMATICS",      "COLLEGE_OF_NATURAL_SCIENCES"),
    "물리학과":             ("PHYSICS",          "COLLEGE_OF_NATURAL_SCIENCES"),
    "화학과":               ("CHEMISTRY",        "COLLEGE_OF_NATURAL_SCIENCES"),
    "패션산업학과":         ("FASHION",          "COLLEGE_OF_NATURAL_SCIENCES"),
    "해양학과":             ("MARINE",           "COLLEGE_OF_NATURAL_SCIENCES"),

    # 사회과학대학
    "사회복지학과":         ("SOCIAL_WELFARE",       "COLLEGE_OF_SOCIAL_SCIENCES"),
    "미디어커뮤니케이션학과": ("MEDIA_COMMUNICATION", "COLLEGE_OF_SOCIAL_SCIENCES"),
    "문헌정보학과":         ("LIBRARY_INFO",         "COLLEGE_OF_SOCIAL_SCIENCES"),
    "창의인재개발학과":     ("CREATIVE_HRD",         "COLLEGE_OF_SOCIAL_SCIENCES"),
    "신문방송학과":         ("MEDIA_COMMUNICATION",  "COLLEGE_OF_SOCIAL_SCIENCES"),  # 구 명칭

    # 글로벌정경대학
    "행정학과":             ("PUBLIC_ADMINISTRATION","COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "정치외교학과":         ("POLITICS_DIPLOMACY",   "COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "경제학과":             ("ECONOMICS",            "COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "경제학과(야)":         ("ECONOMICS",            "COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "Global Trade & Service 학부": ("TRADE",          "COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "Global Trade & Service학부":  ("TRADE",          "COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "무역학부":             ("TRADE",                "COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "무역학부(야)":         ("TRADE",                "COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "소비자학과":           ("CONSUMER_SCIENCE",     "COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "소비자ㆍ아동학과":     ("CONSUMER_SCIENCE",     "COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),  # 구 명칭
    "동북아국제통상전공":   ("NORTHEAST_ASIAN_TRADE","COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "동북아국제통상학부":   ("NORTHEAST_ASIAN_TRADE","COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "동북아국제통상물류학부":("NORTHEAST_ASIAN_TRADE","COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "동북아통상전공":       ("NORTHEAST_ASIAN_TRADE","COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "한국통상전공":         ("NORTHEAST_ASIAN_TRADE","COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "스마트물류공학전공":   ("SMART_LOGISTICS_ENGINEERING","COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),
    "IBE전공":              ("IBE",                  "COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE"),

    # 공과대학
    "에너지화학공학과":     ("ENERGY_CHEMICAL",          "COLLEGE_OF_ENGINEERING"),
    "전기공학과":           ("ELECTRICAL_ENGINEERING",   "COLLEGE_OF_ENGINEERING"),
    "전자공학부":           ("ELECTRONICS_ENGINEERING",  "COLLEGE_OF_ENGINEERING"),
    "전자공학과":           ("ELECTRONICS_ENGINEERING",  "COLLEGE_OF_ENGINEERING"),
    "전자공학과(야)":       ("ELECTRONICS_ENGINEERING",  "COLLEGE_OF_ENGINEERING"),
    "전자공학전공":         ("ELECTRONICS_ENGINEERING",  "COLLEGE_OF_ENGINEERING"),
    "산업경영공학과":       ("INDUSTRIAL_MANAGEMENT",    "COLLEGE_OF_ENGINEERING"),
    "산업경영공학과(야)":   ("INDUSTRIAL_MANAGEMENT",    "COLLEGE_OF_ENGINEERING"),
    "신소재공학과":         ("MATERIAL_SCIENCE",         "COLLEGE_OF_ENGINEERING"),
    "기계공학과":           ("MECHANICAL_ENGINEERING",   "COLLEGE_OF_ENGINEERING"),
    "기계공학과(야)":       ("MECHANICAL_ENGINEERING",   "COLLEGE_OF_ENGINEERING"),
    "바이오-로봇시스템공학과": ("BIO_ROBOTICS_ENGINEERING","COLLEGE_OF_ENGINEERING"),
    "메카트로닉스공학과":   ("BIO_ROBOTICS_ENGINEERING", "COLLEGE_OF_ENGINEERING"),  # 구 명칭
    "안전공학과":           ("SAFETY_ENGINEERING",       "COLLEGE_OF_ENGINEERING"),

    # 정보기술대학
    "컴퓨터공학부":         ("COMPUTER_ENGINEERING",              "COLLEGE_OF_INFORMATION_TECHNOLOGY"),
    "컴퓨터공학부(야)":     ("COMPUTER_ENGINEERING",              "COLLEGE_OF_INFORMATION_TECHNOLOGY"),
    "정보통신공학과":       ("INFORMATION_COMMUNICATION_ENGINEERING","COLLEGE_OF_INFORMATION_TECHNOLOGY"),
    "임베디드시스템공학과": ("EMBEDDED_SYSTEM",                   "COLLEGE_OF_INFORMATION_TECHNOLOGY"),

    # 경영대학
    "경영학부":             ("BUSINESS_ADMINISTRATION","COLLEGE_OF_BUSINESS_ADMINISTRATION"),
    "데이터과학과":         ("DATA_SCIENCE",           "COLLEGE_OF_BUSINESS_ADMINISTRATION"),
    "세무회계학과":         ("TAX_ACCOUNTING",         "COLLEGE_OF_BUSINESS_ADMINISTRATION"),

    # 예술체육대학
    "조형예술학부":         ("FINE_ARTS",          "COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION"),
    "한국화전공":           ("KOREAN_PAINTING",    "COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION"),
    "서양화전공":           ("WESTERN_PAINTING",   "COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION"),
    "디자인학부":           ("DESIGN",             "COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION"),
    "공연예술학과":         ("PERFORMING_ART",     "COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION"),
    "스포츠과학부":         ("SPORTS_SCIENCE",     "COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION"),
    "운동건강학부":         ("HEALTH_EXERCISE",    "COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION"),

    # 사범대학
    "국어교육과":           ("KOREAN_EDUCATION",   "COLLEGE_OF_EDUCATION"),
    "영어교육과":           ("ENGLISH_EDUCATION",  "COLLEGE_OF_EDUCATION"),
    "일어교육과":           ("JAPANESE_EDUCATION", "COLLEGE_OF_EDUCATION"),
    "수학교육과":           ("MATH_EDUCATION",     "COLLEGE_OF_EDUCATION"),
    "체육교육과":           ("PHYSICAL_EDUCATION", "COLLEGE_OF_EDUCATION"),
    "유아교육과":           ("EARLY_CHILDHOOD_EDUCATION","COLLEGE_OF_EDUCATION"),
    "역사교육과":           ("HISTORY_EDUCATION",  "COLLEGE_OF_EDUCATION"),
    "윤리교육과":           ("ETHICS_EDUCATION",   "COLLEGE_OF_EDUCATION"),

    # 도시과학대학
    "도시행정학과":         ("URBAN_ADMINISTRATION",           "COLLEGE_OF_URBAN_SCIENCE"),
    "도시환경공학부":       ("CIVIL_ENVIRONMENT_ENGINEERING",  "COLLEGE_OF_URBAN_SCIENCE"),
    "건설환경공학부":       ("CIVIL_ENVIRONMENT_ENGINEERING",  "COLLEGE_OF_URBAN_SCIENCE"),
    "건설환경공학전공":     ("CIVIL_ENVIRONMENT_ENGINEERING",  "COLLEGE_OF_URBAN_SCIENCE"),
    "환경공학전공":         ("ENVIRONMENT_ENGINEERING",        "COLLEGE_OF_URBAN_SCIENCE"),
    "도시공학과":           ("URBAN_ENGINEERING",              "COLLEGE_OF_URBAN_SCIENCE"),
    "도시건축학부":         ("URBAN_ARCHITECTURE_ARCHITECTURE","COLLEGE_OF_URBAN_SCIENCE"),
    "도시건축학전공":       ("URBAN_ARCHITECTURE_ARCHITECTURE","COLLEGE_OF_URBAN_SCIENCE"),
    "건축공학전공":         ("URBAN_ARCHITECTURE_ENGINEERING", "COLLEGE_OF_URBAN_SCIENCE"),

    # 생명과학기술대학
    "생명과학부":           ("LIFE_SCIENCE",           "COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY"),
    "생명과학전공":         ("LIFE_SCIENCE",           "COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY"),
    "분자의생명전공":       ("LIFE_SCIENCE_MOLECULAR", "COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY"),
    "생명공학부":           ("BIOENGINEERING",         "COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY"),
    "생명공학전공":         ("BIOENGINEERING",         "COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY"),
    "나노바이오공학전공":   ("BIOENGINEERING_NANO",    "COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY"),
    "나노바이오전공":       ("BIOENGINEERING_NANO",    "COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY"),

    # 융합자유전공대학
    "자유전공학부":         ("LIBERAL_ARTS",                   "COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "광전자공학전공(연계)": ("OPTICAL_ELECTRONICS_LINKED",     "COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "물류학전공(연계)":     ("LOGISTICS_LINKED",               "COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "미래교육디자인연계전공":("FUTURE_EDUCATION_DESIGN_LINKED","COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "미래자동차연계전공":   ("FUTURE_CAR_LINKED",              "COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "반도체융합전공":       ("SEMICONDUCTOR_CONVERGENCE",      "COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "소셜데이터사이언스연계전공":("SOCIAL_DATA_SCIENCE_LINKED","COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "인문문화예술기획연계전공":("HUMANITIES_CULTURE_ART_PLANNING_LINKED","COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "지능로봇연계전공":     ("INTELLIGENT_ROBOT_SYSTEM_LINKED","COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "인공지능소프트웨어연계전공":("INTELLIGENT_ROBOT_SYSTEM_LINKED","COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "인공지능·창업연계전공":    ("INTELLIGENT_ROBOT_SYSTEM_LINKED","COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "창의적디자인연계전공": ("CREATIVE_DESIGN_LINKED",         "COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "국제개발협력연계전공": ("LIBERAL_ARTS",                   "COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "MICE,스포츠및관광연계전공":("LIBERAL_ARTS",               "COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),
    "뷰티산업연계전공":     ("LIBERAL_ARTS",                   "COLLEGE_OF_INTERDISCIPLINARY_STUDIES"),

    # 법학부
    "법학부":               ("LAW",               "COLLEGE_OF_NULL"),

    # 구 학과명 / 기타
    "일어일문학과":         ("JAPANESE_EDUCATION", "COLLEGE_OF_EDUCATION"),   # 현 일어교육과
    "체육학부":             ("SPORTS_SCIENCE",     "COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION"),  # 현 스포츠과학부

    # 교양/공통
    "교양":                 ("GENERAL",           "GENERAL"),
    "일선":                 ("GENERAL_ELECTIVE",  "GENERAL"),
    "교직":                 ("TEACHING",          "GENERAL"),
}

# ──────────────────────────────────────────────────────────────────────────────
# 2. 헬퍼 함수
# ──────────────────────────────────────────────────────────────────────────────

def map_department(raw: str) -> tuple[str | None, str | None]:
    """한국어 학과명 → (Department enum name, College enum name)"""
    if not raw or str(raw).strip() == "":
        return None, None
    val = str(raw).strip()
    result = DEPARTMENT_MAP.get(val)
    if result:
        return result
    return None, None


def map_completion_division(raw) -> str:
    if raw is None or str(raw).strip() == "":
        return "UNKNOWN"
    val = str(raw).strip()
    return COMPLETION_DIVISION_MAP.get(val, "UNKNOWN")


def map_grade(raw) -> str:
    if raw is None or str(raw).strip() == "":
        return "UNKNOWN"
    val = str(raw).strip()
    return GRADE_MAP.get(val, "UNKNOWN")


def safe_str(val) -> str | None:
    if val is None or (isinstance(val, float) and pd.isna(val)):
        return None
    s = str(val).strip()
    return s if s and s not in ("-", "nan") else None


def safe_int(val) -> int | None:
    if val is None or (isinstance(val, float) and pd.isna(val)):
        return None
    try:
        return int(float(val))
    except (ValueError, TypeError):
        return None


# ──────────────────────────────────────────────────────────────────────────────
# 3. 엑셀 파일 로드: 헤더 행을 직접 파싱하여 컬럼 위치 탐색
# ──────────────────────────────────────────────────────────────────────────────

TARGET_HEADERS = {
    "년도": "year",
    "학기": "semester",
    "학과(부)": "dept_raw",
    "이수구분": "completion_div_raw",
    "학년": "grade_raw",
    "학수번호": "course_code",
    "교과목명": "title",
    "학점": "credit",
}

all_records: list[dict] = []
unknown_depts: set[str] = set()

for path in sorted(glob.glob("*.xlsx")):
    df_raw = pd.read_excel(path, header=None, dtype=str)
    # Row 0 = 헤더
    header_row = df_raw.iloc[0].tolist()
    col_idx: dict[str, int] = {}
    for col_pos, cell in enumerate(header_row):
        h = str(cell).strip() if cell is not None else ""
        if h in TARGET_HEADERS:
            col_idx[h] = col_pos

    missing = set(TARGET_HEADERS.keys()) - set(col_idx.keys())
    if missing:
        print(f"  ⚠️  {path}: 헤더 누락 {missing} - 건너뜀")
        continue

    data_rows = df_raw.iloc[1:].reset_index(drop=True)
    for _, row in data_rows.iterrows():
        rec: dict = {}
        for kor_header, field_name in TARGET_HEADERS.items():
            ci = col_idx[kor_header]
            rec[field_name] = row.iloc[ci] if ci < len(row) else None
        all_records.append(rec)

df = pd.DataFrame(all_records)
print(f"✅ 총 레코드 수: {len(df):,}")

# 필수 컬럼 정리
df["year"] = pd.to_numeric(df["year"], errors="coerce")
df = df.dropna(subset=["year", "title"])
df["year"] = df["year"].astype(int)
df["title"] = df["title"].astype(str).str.strip()
df["course_code"] = df["course_code"].apply(safe_str)
df["credit"] = df["credit"].apply(safe_int)
df["dept_enum"], df["college_enum"] = zip(*df["dept_raw"].apply(
    lambda x: map_department(safe_str(x))
))
df["completion_div_enum"] = df["completion_div_raw"].apply(
    lambda x: map_completion_division(safe_str(x))
)
df["grade_enum"] = df["grade_raw"].apply(
    lambda x: map_grade(safe_str(x))
)

# 매핑 실패 학과 수집
unmapped = df[df["dept_enum"].isna()]["dept_raw"].dropna().astype(str).str.strip().unique()
for u in sorted(unmapped):
    if u and u not in ("nan", "", "-"):
        unknown_depts.add(u)

# dept/college None인 행 제거 (title+department uniqueness 위해 dept 필수)
df_valid = df.dropna(subset=["dept_enum", "college_enum"]).copy()
print(f"   매핑 성공: {len(df_valid):,}개 / 매핑 실패(제외): {len(df)-len(df_valid):,}개")
if unknown_depts:
    print(f"   ⚠️  미매핑 학과({len(unknown_depts)}개): {sorted(unknown_depts)}")

# ──────────────────────────────────────────────────────────────────────────────
# 4. active 여부 결정: 최신 연도(2025)에 개설된 (title, dept) 집합
# ──────────────────────────────────────────────────────────────────────────────

MAX_YEAR = int(df_valid["year"].max())
print(f"\n📅 최신 연도: {MAX_YEAR}년")

current_pairs: set[tuple[str, str]] = set(
    df_valid[df_valid["year"] == MAX_YEAR][["title", "dept_enum"]]
    .apply(lambda r: (r["title"], r["dept_enum"]), axis=1)
)
current_codes: set[str] = set(
    df_valid[(df_valid["year"] == MAX_YEAR) & df_valid["course_code"].notna()]["course_code"]
)

# ──────────────────────────────────────────────────────────────────────────────
# 5. 중복 제거 & 최종 레코드 구성
#    Unique key: (title, dept_enum)
#    course_code 도 unique → code 중복 시 최신 행 유지
# ──────────────────────────────────────────────────────────────────────────────

# 연도 내림차순 정렬 → groupby first() 로 최신 정보 유지
df_valid = df_valid.sort_values("year", ascending=False)

groups: dict[tuple[str, str], dict] = {}

for _, row in df_valid.iterrows():
    key = (row["title"], row["dept_enum"])
    if key not in groups:
        groups[key] = {
            "course_code":         row["course_code"],
            "title":               row["title"],
            "english_title":       None,          # 엑셀에 없음
            "department":          row["dept_enum"],
            "college":             row["college_enum"],
            "target_grade":        row["grade_enum"],
            "target_term":         "UNKNOWN",     # API 데이터 기준 없음
            "completion_division": row["completion_div_enum"],
            "credit":              row["credit"],
            "content":             None,
            "active":              1 if key in current_pairs else 0,
        }
    else:
        # course_code 가 없으면 나중에 발견된 값으로 보완
        if groups[key]["course_code"] is None and row["course_code"] is not None:
            groups[key]["course_code"] = row["course_code"]

courses: list[dict] = list(groups.values())

# course_code unique 처리: 같은 course_code 가 다른 (title, dept) 에 걸리는 경우
# → 먼저 등록된(최신 연도) 것 유지, 나머지는 code를 None 으로
seen_codes: set[str] = set()
for c in courses:
    if c["course_code"] is not None:
        if c["course_code"] in seen_codes:
            c["course_code"] = None
        else:
            seen_codes.add(c["course_code"])

print(f"✅ 최종 Course 레코드: {len(courses):,}개")
active_cnt = sum(1 for c in courses if c["active"])
print(f"   active=true : {active_cnt:,}개")
print(f"   active=false: {len(courses)-active_cnt:,}개")

# ──────────────────────────────────────────────────────────────────────────────
# 6. SQLite DB 생성
# ──────────────────────────────────────────────────────────────────────────────

DB_PATH = "course.db"
now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# BaseTimeEntity 포함 스키마
cur.executescript("""
PRAGMA journal_mode=WAL;

DROP TABLE IF EXISTS course;

CREATE TABLE course (
    course_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    course_code        TEXT,
    title              TEXT    NOT NULL,
    english_title      TEXT,
    department         TEXT    NOT NULL,
    college            TEXT    NOT NULL,
    target_grade       TEXT,
    target_term        TEXT,
    completion_division TEXT,
    credit             INTEGER,
    content            TEXT,
    active             INTEGER NOT NULL DEFAULT 1,
    created_at         TEXT    NOT NULL,
    updated_at         TEXT    NOT NULL,

    CONSTRAINT uk_course_title_department UNIQUE (title, department),
    CONSTRAINT uk_course_course_code      UNIQUE (course_code)
);
""")

INSERT_SQL = """
INSERT OR IGNORE INTO course
    (course_code, title, english_title, department, college,
     target_grade, target_term, completion_division, credit, content, active,
     created_at, updated_at)
VALUES
    (:course_code, :title, :english_title, :department, :college,
     :target_grade, :target_term, :completion_division, :credit, :content, :active,
     :created_at, :updated_at)
"""

rows = [
    {**c, "created_at": now_iso, "updated_at": now_iso}
    for c in courses
]
cur.executemany(INSERT_SQL, rows)
conn.commit()

inserted = cur.execute("SELECT COUNT(*) FROM course").fetchone()[0]
print(f"\n💾 DB 저장 완료: {DB_PATH}")
print(f"   삽입된 행: {inserted:,}개")

# 간단한 검증 출력
print("\n─── 샘플 (처음 5개) ────────────────────────────────────────────────────")
for row in cur.execute(
    "SELECT course_id, course_code, title, department, college, completion_division, credit, active FROM course LIMIT 5"
).fetchall():
    print(row)

print("\n─── active=false 샘플 5개 ──────────────────────────────────────────────")
for row in cur.execute(
    "SELECT course_id, course_code, title, department, active FROM course WHERE active=0 LIMIT 5"
).fetchall():
    print(row)

print("\n─── 단과대별 강의 수 ───────────────────────────────────────────────────")
for row in cur.execute(
    "SELECT college, COUNT(*) as cnt FROM course GROUP BY college ORDER BY cnt DESC"
).fetchall():
    print(f"  {row[0]:<50} {row[1]:>5}개")

conn.close()
print(f"\n✅ 완료: {DB_PATH} 생성")
