package kr.inuappcenterportal.inuportal.domain.course.crawler.excel;

import static java.util.Map.entry;

import java.util.Map;

/**
 * 과거 강의 엑셀(편람)에는 학과·단과대·이수구분 등이 이름으로만 있어, 학교 API 코드로 바꾼다.
 *
 * <p>개설강의 필터(학과·이수구분·학년 등)는 전부 *_code 컬럼으로 거르므로, 코드가 비면 행이 있어도
 * 필터 결과에서 빠진다. 매핑은 db/manual/courseOffering_enum_migration.sql PHASE 2(실제 편람 값
 * 전수)와 같고, 백필 스크립트 db/manual/course_offering_legacy_code_backfill.sql 도 같은 표를 쓴다.
 * 표에 없는 이름은 null 을 돌려주고, 호출부는 기존 코드를 유지한다.
 */
final class LegacyCourseCodeResolver {

    private LegacyCourseCodeResolver() {
    }

    /** college_name_raw → 코드 (19개) */
    private static final Map<String, String> COLLEGE_CODES = Map.ofEntries(
            entry("인문대학", "A000"),
            entry("자연과학대학", "B000"),
            entry("사회과학대학", "C000"),
            entry("공과대학", "E000"),
            entry("정보기술대학", "I000"),
            entry("경영대학", "J000"),
            entry("교양", "X000"),
            entry("일선", "W000"),
            entry("교직", "Y000"),
            entry("군사학", "Z000"),
            entry("기타", "V000"),
            entry("글로벌정경대학", "0000689"),
            entry("예술체육대학", "0000190"),
            entry("사범대학", "0000063"),
            entry("도시과학대학", "0000033"),
            entry("생명과학기술대학", "0000182"),
            entry("융합자유전공대학", "0000837"),
            entry("단과대구분없음", "0000465"),
            entry("단과대구분없음(법학)", "0000706")
    );

    /** hy_name_raw → 코드 (5개) */
    private static final Map<String, String> HY_CODES = Map.ofEntries(
            entry("전학년", "0"),
            entry("1", "1"),
            entry("2", "2"),
            entry("3", "3"),
            entry("4", "4")
    );

    /** isu_name_raw → 코드 (9개) */
    private static final Map<String, String> ISU_CODES = Map.ofEntries(
            entry("기초교양", "11"),
            entry("핵심교양", "21"),
            entry("심화교양", "23"),
            entry("전공기초", "25"),
            entry("전공핵심", "31"),
            entry("전공심화", "41"),
            entry("교직", "50"),
            entry("군사학", "70"),
            entry("일반선택", "80")
    );

    /** cnctr_isu_name_raw → 코드 (3개) */
    private static final Map<String, String> CNCTR_ISU_CODES = Map.ofEntries(
            entry("일반(1~15주)", "0"),
            entry("집중A(1~8주)", "1"),
            entry("집중C(1~12주)", "3")
    );

    /** isu_fld_name_raw → 코드 (20개) */
    private static final Map<String, String> ISU_FLD_CODES = Map.ofEntries(
            entry("학문의기초", "161"),
            entry("기초과학ㆍ공학", "162"),
            entry("기초과학·공학", "162"),
            entry("(핵심)INU세미나", "171"),
            entry("(핵심)인문", "172"),
            entry("(핵심)사회", "173"),
            entry("(핵심)과학기술", "174"),
            entry("(핵심)예술체육", "175"),
            entry("(핵심)외국어", "176"),
            entry("인문", "182"),
            entry("사회", "183"),
            entry("과학기술", "184"),
            entry("예술체육", "185"),
            entry("외국어", "186"),
            entry("전공기초", "31"),
            entry("전공핵심", "34"),
            entry("전공심화", "35"),
            entry("교직", "51"),
            entry("군사학", "71"),
            entry("일반선택", "81")
    );

    /** ssup_type_name_raw → 코드 (20개) */
    private static final Map<String, String> SSUP_TYPE_CODES = Map.ofEntries(
            entry("강의(이론)", "1"),
            entry("실험실습", "2"),
            entry("체육실기", "3"),
            entry("미술실기", "4"),
            entry("이론실험실습", "5"),
            entry("열린사이버대학(OCU)", "7"),
            entry("e-Learning", "8"),
            entry("담장너머~,사회봉사(1)", "11"),
            entry("사회봉사(2)", "12"),
            entry("사회봉사(3)", "13"),
            entry("자기설계세미나", "17"),
            entry("이론(어학)", "20"),
            entry("RISE(시간표 있음)", "21"),
            entry("RISE(시간표 없음)", "22"),
            entry("예술체육실기", "23"),
            entry("온라인혼합형강좌", "24"),
            entry("K-MOOC", "25"),
            entry("e-Learning(HUSS)", "26"),
            entry("온라인혼합형강좌(HUSS)", "27"),
            entry("현장형(HUSS)", "28")
    );

    /** dept_name_raw → 코드 (101개) */
    private static final Map<String, String> DEPT_CODES = Map.ofEntries(
            entry("국어국문학과", "AIA1"),
            entry("영어영문학과", "AIB1"),
            entry("중어중국학과", "AID1"),
            entry("독어독문학과", "AIE1"),
            entry("불어불문학과", "AIF1"),
            entry("일본지역문화학과", "0000793"),
            entry("일어일문학과", "0000793"),
            entry("수학과", "BKA1"),
            entry("물리학과", "BKB1"),
            entry("화학과", "BKC1"),
            entry("패션산업학과", "BLB1"),
            entry("해양학과", "0000189"),
            entry("사회복지학과", "0000144"),
            entry("미디어커뮤니케이션학과", "0000794"),
            entry("신문방송학과", "0000794"),
            entry("문헌정보학과", "0000053"),
            entry("창의인재개발학과", "0000054"),
            entry("행정학과", "0000698"),
            entry("정치외교학과", "0000699"),
            entry("경제학과", "0000700"),
            entry("경제학과(야)", "0000701"),
            entry("무역학부", "0000913"),
            entry("Global Trade & Service학부", "0000913"),
            entry("무역학부(야)", "0000703"),
            entry("소비자학과", "0000704"),
            entry("소비자ㆍ아동학과", "0000704"),
            entry("에너지화학공학과", "0000055"),
            entry("전기공학과", "EPB1"),
            entry("전자공학부", "0000813"),
            entry("전자공학과", "EPC1"),
            entry("전자공학과(야)", "EPC1"),
            entry("전자공학전공", "0000828"),
            entry("산업경영공학과", "EPG1"),
            entry("산업경영공학과(야)", "EPG1"),
            entry("신소재공학과", "0000076"),
            entry("기계공학과", "0000459"),
            entry("기계공학과(야)", "0000459"),
            entry("메카트로닉스공학과", "0000814"),
            entry("바이오-로봇시스템공학과", "0000814"),
            entry("안전공학과", "0000075"),
            entry("컴퓨터공학부", "0000077"),
            entry("컴퓨터공학부(야)", "0000077"),
            entry("정보통신공학과", "IAB1"),
            entry("임베디드시스템공학과", "0000042"),
            entry("경영학부", "JA01"),
            entry("데이터과학과", "0000812"),
            entry("세무회계학과", "0000057"),
            entry("조형예술학부", "0000192"),
            entry("한국화전공", "0000193"),
            entry("서양화전공", "0000194"),
            entry("디자인학부", "0000195"),
            entry("공연예술학과", "0000196"),
            entry("스포츠과학부", "0000815"),
            entry("체육학부", "0000815"),
            entry("운동건강학부", "0000191"),
            entry("국어교육과", "0000064"),
            entry("영어교육과", "0000065"),
            entry("일어교육과", "0000066"),
            entry("수학교육과", "0000067"),
            entry("체육교육과", "0000068"),
            entry("유아교육과", "0000069"),
            entry("역사교육과", "0000070"),
            entry("윤리교육과", "0000071"),
            entry("도시행정학과", "0000073"),
            entry("건설환경공학전공", "0000156"),
            entry("건설환경공학부", "0000156"),
            entry("환경공학전공", "0000157"),
            entry("도시환경공학부", "0000034"),
            entry("도시공학과", "0000463"),
            entry("도시건축학부", "0000038"),
            entry("건축공학전공", "0000160"),
            entry("도시건축학전공", "0000464"),
            entry("생명과학부", "0000183"),
            entry("생명과학전공", "0000184"),
            entry("분자의생명전공", "0000185"),
            entry("생명공학부", "0000186"),
            entry("생명공학전공", "0000187"),
            entry("나노바이오공학전공", "0000833"),
            entry("나노바이오전공", "0000833"),
            entry("자유전공학부", "0000838"),
            entry("법학부", "0000707"),
            entry("IBE전공", "0000832"),
            entry("한국통상전공", "0000832"),
            entry("스마트물류공학전공", "0000818"),
            entry("동북아국제통상전공", "0000817"),
            entry("동북아통상전공", "0000817"),
            entry("동북아국제통상학부", "0000817"),
            entry("반도체융합전공", "0000829"),
            entry("광전자공학전공(연계)", "VAB1"),
            entry("물류학전공(연계)", "VAC1"),
            entry("미래교육디자인연계전공", "0000849"),
            entry("미래자동차연계전공", "0000789"),
            entry("소셜데이터사이언스연계전공", "0000678"),
            entry("인문문화예술기획연계전공", "0000677"),
            entry("지능형로봇시스템연계전공", "0000912"),
            entry("지능로봇연계전공", "0000912"),
            entry("창의적디자인연계전공", "0000616"),
            entry("교양", "XAA0"),
            entry("일선", "WAA0"),
            entry("교직", "YAA0"),
            entry("군사학", "ZAA0"),
            entry("HUSS(타대학)", "VEA1"),
            entry("HUSS포용사회이니셔티브학부", "VE00")
    );

    /** 학과(부) 이름 → 코드. 모르는 이름이면 null. */
    static String deptCode(String name) {
        return lookup(DEPT_CODES, name);
    }

    /** 소속분류(단과대) 이름 → 코드. 모르는 이름이면 null. */
    static String collegeCode(String name) {
        return lookup(COLLEGE_CODES, name);
    }

    /** 학년 이름 → 코드. 모르는 이름이면 null. */
    static String hyCode(String name) {
        return lookup(HY_CODES, name);
    }

    /** 이수구분 이름 → 코드. 모르는 이름이면 null. */
    static String isuCode(String name) {
        return lookup(ISU_CODES, name);
    }

    /** 이수영역 이름 → 코드. 모르는 이름이면 null. */
    static String isuFldCode(String name) {
        return lookup(ISU_FLD_CODES, name);
    }

    /** 수업유형 이름 → 코드. 모르는 이름이면 null. */
    static String ssupTypeCode(String name) {
        return lookup(SSUP_TYPE_CODES, name);
    }

    /** 집중이수제구분 이름 → 코드. 모르는 이름이면 null. */
    static String cnctrIsuCode(String name) {
        return lookup(CNCTR_ISU_CODES, name);
    }

    private static String lookup(Map<String, String> codes, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return codes.get(name.trim());
    }
}
