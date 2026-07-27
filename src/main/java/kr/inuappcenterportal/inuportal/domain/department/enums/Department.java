package kr.inuappcenterportal.inuportal.domain.department.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Department {

    // 인문대학
    KOREAN("국어국문학과", College.COLLEGE_OF_HUMANITIES, null, null),
    ENGLISH("영어영문학과", College.COLLEGE_OF_HUMANITIES,
            "https://english.inu.ac.kr/ui/1973/subview.do",
            "https://english.inu.ac.kr/ui/1972/subview.do"),
    GERMAN("독어독문학과", College.COLLEGE_OF_HUMANITIES,
            "https://german.inu.ac.kr/german/1822/subview.do",
            "https://german.inu.ac.kr/german/1821/subview.do"),
    FRENCH("불어불문학과", College.COLLEGE_OF_HUMANITIES,
            "https://inufrance.inu.ac.kr/inufrance/1901/subview.do",
            null),
    JAPANESE("일본지역문화학과", College.COLLEGE_OF_HUMANITIES,
            "https://unjapan.inu.ac.kr/unjapan/2038/subview.do",
            null),
    CHINESE("중어중국학과", College.COLLEGE_OF_HUMANITIES,
            "https://inuchina.inu.ac.kr/inuchina/2087/subview.do",
            "https://inuchina.inu.ac.kr/inuchina/2086/subview.do"),

    // 자연과학대학
    MATHEMATICS("수학과", College.COLLEGE_OF_NATURAL_SCIENCES,
            "https://math.inu.ac.kr/isu/2214/subview.do",
            "https://math.inu.ac.kr/isu/2213/subview.do"),
    PHYSICS("물리학과", College.COLLEGE_OF_NATURAL_SCIENCES,
            "https://physics.inu.ac.kr/physics/2151/subview.do",
            "https://physics.inu.ac.kr/physics/2150/subview.do"),
    CHEMISTRY("화학과", College.COLLEGE_OF_NATURAL_SCIENCES,
            "https://chem.inu.ac.kr/chem/2403/subview.do",
            "https://chem.inu.ac.kr/chem/2402/subview.do"),
    FASHION("패션산업학과", College.COLLEGE_OF_NATURAL_SCIENCES,
            "https://uifashion.inu.ac.kr/uifashion/2282/subview.do",
            "https://uifashion.inu.ac.kr/uifashion/2280/subview.do"),
    MARINE("해양학과", College.COLLEGE_OF_NATURAL_SCIENCES,
            "https://marine.inu.ac.kr/marine/13622/subview.do",
            "https://marine.inu.ac.kr/marine/2316/subview.do"),

    // 사회과학대학
    SOCIAL_WELFARE("사회복지학과", College.COLLEGE_OF_SOCIAL_SCIENCES,
            "https://dsw.inu.ac.kr/dsw/12160/subview.do",
            "https://dsw.inu.ac.kr/dsw/2489/subview.do"),
    MEDIA_COMMUNICATION("미디어커뮤니케이션학과", College.COLLEGE_OF_SOCIAL_SCIENCES,
            "https://newdays.inu.ac.kr/shinbang/2534/subview.do",
            "https://newdays.inu.ac.kr/shinbang/2533/subview.do"),
    LIBRARY_INFO("문헌정보학과", College.COLLEGE_OF_SOCIAL_SCIENCES,
            "https://cls.inu.ac.kr/cls/2444/subview.do",
            "https://cls.inu.ac.kr/cls/2443/subview.do"),
    CREATIVE_HRD("창의인재개발학과", College.COLLEGE_OF_SOCIAL_SCIENCES,
            "https://hrd.inu.ac.kr/hrd/2577/subview.do",
            "https://hrd.inu.ac.kr/hrd/2576/subview.do"),

    // 글로벌정경대학
    PUBLIC_ADMINISTRATION("행정학과", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE,
            "https://uipa.inu.ac.kr/uipa/7797/subview.do",
            "https://uipa.inu.ac.kr/uipa/7796/subview.do"),
    POLITICS_DIPLOMACY("정치외교학과", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE,
            "https://politics.inu.ac.kr/politics/2739/subview.do",
            "https://politics.inu.ac.kr/politics/2738/subview.do"),
    ECONOMICS("경제학과", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE,
            "https://econ.inu.ac.kr/econ/2638/subview.do",
            "https://econ.inu.ac.kr/econ/2637/subview.do"),
    TRADE("Global Trade & Service 학부", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE,
            "https://trade.inu.ac.kr/trade/2701/subview.do",
            "https://trade.inu.ac.kr/trade/2686/subview.do"),
    CONSUMER_SCIENCE("소비자학과", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE,
            "https://ccs.inu.ac.kr/ccs/2809/subview.do",
            "https://ccs.inu.ac.kr/ccs/2807/subview.do"),

    // 공과대학
    ENERGY_CHEMICAL("에너지화학공학과", College.COLLEGE_OF_ENGINEERING,
            null,
            "https://energy.inu.ac.kr/energy/3254/subview.do"),
    ELECTRICAL_ENGINEERING("전기공학과", College.COLLEGE_OF_ENGINEERING,
            "https://elec.inu.ac.kr/elec/3317/subview.do",
            "https://elec.inu.ac.kr/elec/3316/subview.do"),
    ELECTRONICS_ENGINEERING("전자공학부", College.COLLEGE_OF_ENGINEERING,
            "https://ee.inu.ac.kr/electron/12184/subview.do",
            "https://ee.inu.ac.kr/electron/12199/subview.do"),
    INDUSTRIAL_MANAGEMENT("산업경영공학과", College.COLLEGE_OF_ENGINEERING,
            "https://ime.inu.ac.kr/ime/3094/subview.do",
            "https://ime.inu.ac.kr/ime/3093/subview.do"),
    MATERIAL_SCIENCE("신소재공학과", College.COLLEGE_OF_ENGINEERING,
            "https://mse.inu.ac.kr/mse/3139/subview.do",
            "https://mse.inu.ac.kr/mse/3137/subview.do"),
    MECHANICAL_ENGINEERING("기계공학과", College.COLLEGE_OF_ENGINEERING,
            "https://me.inu.ac.kr/me/12171/subview.do",
            "https://me.inu.ac.kr/me/2980/subview.do"),
    BIO_ROBOTICS_ENGINEERING("바이오-로봇시스템공학과", College.COLLEGE_OF_ENGINEERING,
            "https://bio-robot.inu.ac.kr/meca/3045/subview.do",
            "https://bio-robot.inu.ac.kr/meca/3045/subview.do"),
    SAFETY_ENGINEERING("안전공학과", College.COLLEGE_OF_ENGINEERING,
            "https://safety.inu.ac.kr/safety/12167/subview.do",
            "https://safety.inu.ac.kr/safety/3195/subview.do"),

    // 정보기술대학
    COMPUTER_ENGINEERING("컴퓨터공학부", College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
            "https://cse.inu.ac.kr/isis/12172/subview.do",
            "https://cse.inu.ac.kr/isis/3521/subview.do"),
    INFORMATION_COMMUNICATION_ENGINEERING("정보통신공학과", College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
            "https://ite.inu.ac.kr/ite/3467/subview.do",
            "https://ite.inu.ac.kr/ite/3466/subview.do"),
    EMBEDDED_SYSTEM("임베디드시스템공학과", College.COLLEGE_OF_INFORMATION_TECHNOLOGY,
            "https://ese.inu.ac.kr/ese/3422/subview.do",
            "https://ese.inu.ac.kr/ese/3421/subview.do"),

    // 경영대학
    BUSINESS_ADMINISTRATION("경영학부", College.COLLEGE_OF_BUSINESS_ADMINISTRATION,
            "https://biz.inu.ac.kr/biz/3605/subview.do",
            "https://biz.inu.ac.kr/biz/3604/subview.do"),
    DATA_SCIENCE("데이터과학과", College.COLLEGE_OF_BUSINESS_ADMINISTRATION,
            "https://datascience.inu.ac.kr/datascience/3708/subview.do",
            "https://datascience.inu.ac.kr/datascience/3707/subview.do"),
    TAX_ACCOUNTING("세무회계학과", College.COLLEGE_OF_BUSINESS_ADMINISTRATION,
            "https://tax.inu.ac.kr/tax/3658/subview.do",
            "https://tax.inu.ac.kr/tax/3657/subview.do"),

    // 예술체육대학
    FINE_ARTS("조형예술학부", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION,
            null,
            "https://finearts.inu.ac.kr/finearts/11426/subview.do"),
    KOREAN_PAINTING("한국화전공", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION,
            null,
            "https://finearts.inu.ac.kr/finearts/4152/subview.do"),
    WESTERN_PAINTING("서양화전공", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION,
            null,
            "https://finearts.inu.ac.kr/finearts/4153/subview.do"),
    DESIGN("디자인학부", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION,
            "https://design.inu.ac.kr/design/4010/subview.do",
            "https://design.inu.ac.kr/design/4009/subview.do"),
    PERFORMING_ART("공연예술학과", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION,
            "https://uipa10.inu.ac.kr/uipa10/3951/subview.do",
            "https://uipa10.inu.ac.kr/uipa10/3950/subview.do"),
    SPORTS_SCIENCE("스포츠과학부", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION,
            "https://sports.inu.ac.kr/sub3_2.php",
            "https://sports.inu.ac.kr/sub3_2.php"),
    HEALTH_EXERCISE("운동건강학부", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION,
            "https://uiex.inu.ac.kr/uiex/4060/subview.do",
            "https://uiex.inu.ac.kr/uiex/4059/subview.do"),

    // 사범대학
    KOREAN_EDUCATION("국어교육과", College.COLLEGE_OF_EDUCATION,
            "https://edukorean.inu.ac.kr/edukorean/4238/subview.do",
            "https://edukorean.inu.ac.kr/edukorean/4237/subview.do"),
    ENGLISH_EDUCATION("영어교육과", College.COLLEGE_OF_EDUCATION,
            "https://eduenglish.inu.ac.kr/eduenglish/4412/subview.do",
            "https://eduenglish.inu.ac.kr/eduenglish/4411/subview.do"),
    JAPANESE_EDUCATION("일어교육과", College.COLLEGE_OF_EDUCATION,
            "https://edujapanese.inu.ac.kr/edujapanese/4598/subview.do",
            "https://edujapanese.inu.ac.kr/edujapanese/4597/subview.do"),
    MATH_EDUCATION("수학교육과", College.COLLEGE_OF_EDUCATION,
            "https://mathedu.inu.ac.kr/edumath/4301/subview.do",
            "https://mathedu.inu.ac.kr/edumath/4300/subview.do"),
    PHYSICAL_EDUCATION("체육교육과", College.COLLEGE_OF_EDUCATION,
            "https://eduphysical.inu.ac.kr/eduphysical/4647/subview.do",
            "https://eduphysical.inu.ac.kr/eduphysical/4646/subview.do"),
    EARLY_CHILDHOOD_EDUCATION("유아교육과", College.COLLEGE_OF_EDUCATION,
            "https://ece.inu.ac.kr/ece/4477/subview.do",
            "https://ece.inu.ac.kr/ece/4477/subview.do"),
    HISTORY_EDUCATION("역사교육과", College.COLLEGE_OF_EDUCATION,
            "https://eduhistory.inu.ac.kr/eduhistory/7990/subview.do",
            "https://eduhistory.inu.ac.kr/eduhistory/7989/subview.do"),
    ETHICS_EDUCATION("윤리교육과", College.COLLEGE_OF_EDUCATION,
            "https://eduethics.inu.ac.kr/eduethics/4535/subview.do",
            "https://eduethics.inu.ac.kr/eduethics/4534/subview.do"),

    // 도시과학대학
    URBAN_ADMINISTRATION("도시행정학과", College.COLLEGE_OF_URBAN_SCIENCE,
            "https://urban.inu.ac.kr/urban/4885/subview.do",
            "https://urban.inu.ac.kr/urban/4884/subview.do"),
    CIVIL_ENVIRONMENT_ENGINEERING("도시환경공학부(건설환경공학전공)", College.COLLEGE_OF_URBAN_SCIENCE,
            "https://civil.inu.ac.kr/civil/4705/subview.do",
            "https://civil.inu.ac.kr/civil/4704/subview.do"),
    ENVIRONMENT_ENGINEERING("도시환경공학부(환경공학전공)", College.COLLEGE_OF_URBAN_SCIENCE,
            "https://et.inu.ac.kr/et/7721/subview.do",
            "https://et.inu.ac.kr/et/7720/subview.do"),
    URBAN_ENGINEERING("도시공학과", College.COLLEGE_OF_URBAN_SCIENCE,
            "https://scity.inu.ac.kr/ucv/4747/subview.do",
            "https://scity.inu.ac.kr/ucv/4746/subview.do"),
    URBAN_ARCHITECTURE_ENGINEERING("도시건축학부(건축공학전공)", College.COLLEGE_OF_URBAN_SCIENCE,
            "https://archi.inu.ac.kr/archi/4841/subview.do",
            "https://archi.inu.ac.kr/archi/4839/subview.do"),
    URBAN_ARCHITECTURE_ARCHITECTURE("도시건축학부(도시건축학전공)", College.COLLEGE_OF_URBAN_SCIENCE,
            "https://archi.inu.ac.kr/archi/4842/subview.do",
            "https://archi.inu.ac.kr/archi/4840/subview.do"),

    // 생명과학기술대학
    LIFE_SCIENCE("생명과학부(생명과학전공)", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY,
            "https://life.inu.ac.kr/life/4961/subview.do",
            "https://life.inu.ac.kr/life/4960/subview.do"),
    LIFE_SCIENCE_MOLECULAR("생명과학부(분자의생명전공)", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY,
            "https://molbio.inu.ac.kr/molbio/5005/subview.do",
            "https://molbio.inu.ac.kr/molbio/5004/subview.do"),
    BIOENGINEERING("생명공학부(생명공학전공)", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY,
            "https://bioeng.inu.ac.kr/engineeringlife/5127/subview.do",
            "https://bioeng.inu.ac.kr/engineeringlife/5126/subview.do"),
    BIOENGINEERING_NANO("생명공학부(나노바이오공학전공)", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY,
            "https://www.inu.ac.kr/nanobio/12168/subview.do",
            "https://www.inu.ac.kr/nanobio/12157/subview.do"),

    // 융합자유전공대학
    LIBERAL_ARTS("자유전공학부", College.COLLEGE_OF_INTERDISCIPLINARY_STUDIES,
            null, null),
    // 자유전공학부에 국제자유전공학부, 융합학부가 포함되어있음. 교육과정이 따로 존재하지 않음
    //INTERNATIONAL_LIBERAL_ARTS("국제자유전공학부", College.COLLEGE_OF_INTERDISCIPLINARY_STUDIES, null, null),
    //CONVERGENCE("융합학부", College.COLLEGE_OF_INTERDISCIPLINARY_STUDIES, null, null),

    // 동북아국제통상물류학부
    NORTHEAST_ASIAN_TRADE("동북아국제통상전공", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE,
            "https://www.inu.ac.kr/nas/3792/subview.do", null),
    SMART_LOGISTICS_ENGINEERING("스마트물류공학전공", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE,
            "https://slog.inu.ac.kr/slog/3837/subview.do",
            "https://slog.inu.ac.kr/slog/3836/subview.do"),
    IBE("IBE전공", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE,
            null, null),

    // 법학부
    LAW("법학부", College.COLLEGE_OF_NULL,
            "https://law.inu.ac.kr/law/5177/subview.do",
            "https://law.inu.ac.kr/law/5176/subview.do"),

    // 교양
    GENERAL("교양", College.GENERAL, null, null);


    // 계약학과
    // 도시과학대학-도시건걸공학과
    // 경영대학 테크노경영학과
    // 글로벌정경대학 글로벌무역물류학과

    private final String departmentName;    // 학과명
    private final College collegeName;      // 단과대명
    private final String courseOverviewUrl; // 교과목개요 Url
    private final String curriculumUrl;     // 교육과정 Url

    public static Department from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmedValue = value.trim();

        for (Department department : values()) {
            if (department.name().equalsIgnoreCase(trimmedValue)
                    || department.departmentName.equals(trimmedValue)) {
                return department;
            }
        }

        throw new IllegalArgumentException("존재하지 않는 학과입니다: " + value);
    }
}
