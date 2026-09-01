"""
find_discontinued_courses.py
----------------------------
학수번호를 기준으로 과거에는 개설되었지만
가장 최근 연도(2025년)에는 개설되지 않은 강의를 찾아냅니다.
"""

import pandas as pd
import glob
import warnings
from collections import defaultdict

warnings.filterwarnings("ignore")

# ──────────────────────────────────────────
# 1. 모든 엑셀 파일 로드 & 통합
# ──────────────────────────────────────────
COLS = {
    2: "년도",
    3: "학기",
    4: "소속분류",
    5: "학과(부)",
    6: "이수구분",
    8: "학수번호",
    9: "교과목명",
    10: "수업방법",
    11: "담당교수",
    14: "학점",
}

records = []
for path in sorted(glob.glob("*.xlsx")):
    df_raw = pd.read_excel(path, header=None)
    # Row 0 = 헤더, Row 1~ = 데이터
    data = df_raw.iloc[1:].reset_index(drop=True)
    for idx, row in data.iterrows():
        rec = {kor: row[col] for col, kor in COLS.items()}
        records.append(rec)

df = pd.DataFrame(records)

# 타입 정리
df["년도"] = pd.to_numeric(df["년도"], errors="coerce")
df = df.dropna(subset=["년도", "학수번호"])
df["년도"] = df["년도"].astype(int)
df["학수번호"] = df["학수번호"].astype(str).str.strip()
df["교과목명"] = df["교과목명"].astype(str).str.strip()

print(f"총 강의 레코드 수: {len(df):,}개")
print(f"   포함 년도: {sorted(df['년도'].unique())}")
print(f"   포함 학기: {sorted(df['학기'].unique())}")
print()

# ──────────────────────────────────────────
# 2. 기준 설정: "현재" vs "과거"
# ──────────────────────────────────────────
MAX_YEAR = df["년도"].max()          # 최근 연도 (2025)
PAST_YEARS = df["년도"].unique()     # 모든 연도

# 최근 연도에 개설된 학수번호 집합
current_codes = set(
    df[df["년도"] == MAX_YEAR]["학수번호"].unique()
)

# 과거(최근 연도 제외)에 개설된 학수번호 집합
past_codes = set(
    df[df["년도"] < MAX_YEAR]["학수번호"].unique()
)

# 과거에는 있었지만 현재 없는 학수번호
discontinued_codes = past_codes - current_codes

print(f"{MAX_YEAR}년 기준 현재 개설 학수번호: {len(current_codes):,}개")
print(f"과거({df['년도'].min()}~{MAX_YEAR-1}) 개설 학수번호: {len(past_codes):,}개")
print(f"현재 존재하지 않는(폐지된) 학수번호: {len(discontinued_codes):,}개")
print()

# ──────────────────────────────────────────
# 3. 폐지된 강의 상세 정보 수집
# ──────────────────────────────────────────
past_df = df[df["학수번호"].isin(discontinued_codes)].copy()

# 각 학수번호별 마지막 개설 연도·학기, 교과목명, 학과 등 정리
summary_rows = []
for code, grp in past_df.groupby("학수번호"):
    # 가장 최근 개설 정보
    last_row = grp.sort_values(["년도", "학기"], ascending=False).iloc[0]
    last_year = int(last_row["년도"])
    last_sem  = last_row["학기"]
    name      = last_row["교과목명"]
    dept      = last_row["학과(부)"]
    isu       = last_row["이수구분"]
    credit    = last_row["학점"]
    prof      = last_row["담당교수"]

    # 개설된 연도 목록
    years_offered = sorted(grp["년도"].unique().tolist())

    summary_rows.append({
        "학수번호":       code,
        "교과목명":       name,
        "학과(부)":       dept,
        "이수구분":       isu,
        "학점":           credit,
        "마지막개설년도": last_year,
        "마지막개설학기": last_sem,
        "마지막담당교수": prof,
        "개설연도목록":   ", ".join(map(str, years_offered)),
        "총개설횟수":     len(grp),
    })

result_df = pd.DataFrame(summary_rows).sort_values(
    ["마지막개설년도", "마지막개설학기", "학수번호"],
    ascending=[False, True, True]
).reset_index(drop=True)

# ──────────────────────────────────────────
# 4. 출력
# ──────────────────────────────────────────
print("=" * 80)
print(f"  현재({MAX_YEAR}년) 존재하지 않는 강의 목록 (마지막 개설 연도 내림차순)")
print("=" * 80)

pd.set_option("display.max_rows", 200)
pd.set_option("display.max_columns", 20)
pd.set_option("display.width", 120)
pd.set_option("display.max_colwidth", 25)

print(result_df[[
    "학수번호", "교과목명", "학과(부)", "이수구분", "학점",
    "마지막개설년도", "마지막개설학기", "마지막담당교수", "총개설횟수"
]].to_string(index=True))

# ──────────────────────────────────────────
# 5. 마지막 개설 연도별 집계
# ──────────────────────────────────────────
print()
print("-" * 60)
print("마지막 개설 연도별 폐지 강의 수:")
print("-" * 60)
year_counts = result_df.groupby("마지막개설년도")["학수번호"].count().sort_index(ascending=False)
for yr, cnt in year_counts.items():
    print(f"  {yr}년 이후 폐지: {cnt}개")

# ──────────────────────────────────────────
# 6. 엑셀 저장
# ──────────────────────────────────────────
out_path = "폐지강의목록.xlsx"
with pd.ExcelWriter(out_path, engine="openpyxl") as writer:
    result_df.to_excel(writer, sheet_name="폐지강의목록", index=True)

    # 연도별 통계도 함께 저장
    stats_df = year_counts.reset_index()
    stats_df.columns = ["마지막개설년도", "폐지강의수"]
    stats_df.to_excel(writer, sheet_name="연도별통계", index=False)

print()
print(f"결과 저장 완료: {out_path}")
