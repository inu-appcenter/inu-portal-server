package kr.inuappcenterportal.inuportal.domain.department.enums;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

@Getter
public enum Department {

    /// 총 64개의 학과
    // 인문대학(6개)
    KOREAN("국어국문학과", "AIA1", College.COLLEGE_OF_HUMANITIES, null, null, "https://korean.inu.ac.kr/korean/1780/subview.do", "https://www.inu.ac.kr/bbs/isis/286/rssList.do?row=10", true, true, true),
    ENGLISH("영어영문학과", "AIB1", College.COLLEGE_OF_HUMANITIES, "https://english.inu.ac.kr/ui/1973/subview.do", "https://english.inu.ac.kr/ui/1972/subview.do", "https://english.inu.ac.kr/ui/7888/subview.do", "https://www.inu.ac.kr/bbs/isis/642/rssList.do?row=10", true, true, true),
    GERMAN("독어독문학과", "AIE1", College.COLLEGE_OF_HUMANITIES, "https://german.inu.ac.kr/german/1822/subview.do", "https://german.inu.ac.kr/german/1821/subview.do", "https://german.inu.ac.kr/german/1841/subview.do", "https://www.inu.ac.kr/bbs/isis/289/rssList.do?row=10", true, true, true),
    FRENCH("불어불문학과", "AIF1", College.COLLEGE_OF_HUMANITIES, "https://inufrance.inu.ac.kr/inufrance/1901/subview.do", null, "https://inufrance.inu.ac.kr/inufrance/1915/subview.do", "https://www.inu.ac.kr/bbs/isis/292/rssList.do?row=10", true, true, true),
    JAPANESE("일본지역문화학과", "0000793", College.COLLEGE_OF_HUMANITIES, "https://unjapan.inu.ac.kr/unjapan/2038/subview.do", null, "https://unjapan.inu.ac.kr/unjapan/2025/subview.do", "https://www.inu.ac.kr/bbs/isis/298/rssList.do?row=10", true, true, true),
    CHINESE("중어중국학과", "AID1", College.COLLEGE_OF_HUMANITIES, "https://inuchina.inu.ac.kr/inuchina/2087/subview.do", "https://inuchina.inu.ac.kr/inuchina/2086/subview.do", "https://inuchina.inu.ac.kr/inuchina/2096/subview.do", "https://www.inu.ac.kr/bbs/isis/301/rssList.do?row=10", true, true, true),

    // 자연과학대학(5개)
    MATHEMATICS("수학과", "BKA1", College.COLLEGE_OF_NATURAL_SCIENCES, "https://math.inu.ac.kr/isu/2214/subview.do", "https://math.inu.ac.kr/isu/2213/subview.do", "https://math.inu.ac.kr/isu/2219/subview.do", "https://www.inu.ac.kr/bbs/isis/307/rssList.do?row=10", true, true, true),
    PHYSICS("물리학과", "BKB1", College.COLLEGE_OF_NATURAL_SCIENCES, "https://physics.inu.ac.kr/physics/2151/subview.do", "https://physics.inu.ac.kr/physics/2150/subview.do", "https://physics.inu.ac.kr/physics/2155/subview.do", "https://www.inu.ac.kr/bbs/isis/304/rssList.do?row=10", true, true, true),
    CHEMISTRY("화학과", "BKC1", College.COLLEGE_OF_NATURAL_SCIENCES, "https://chem.inu.ac.kr/chem/2403/subview.do", "https://chem.inu.ac.kr/chem/2402/subview.do", "https://chem.inu.ac.kr/chem/2389/subview.do", "https://www.inu.ac.kr/bbs/isis/316/rssList.do?row=10", true, true, true),
    FASHION("패션산업학과", "BLB1", College.COLLEGE_OF_NATURAL_SCIENCES, "https://uifashion.inu.ac.kr/uifashion/2282/subview.do", "https://uifashion.inu.ac.kr/uifashion/2280/subview.do", "https://uifashion.inu.ac.kr/uifashion/2271/subview.do", "https://www.inu.ac.kr/bbs/isis/1735/rssList.do?row=10", true, true, true),
    MARINE("해양학과", "0000189", College.COLLEGE_OF_NATURAL_SCIENCES, "https://marine.inu.ac.kr/marine/13622/subview.do", "https://marine.inu.ac.kr/marine/2316/subview.do", "https://marine.inu.ac.kr/marine/2323/subview.do", "https://www.inu.ac.kr/bbs/isis/313/rssList.do?row=10", true, true, true),

    // 사회과학대학(4개)
    SOCIAL_WELFARE("사회복지학과", "0000144", College.COLLEGE_OF_SOCIAL_SCIENCES, "https://dsw.inu.ac.kr/dsw/12160/subview.do", "https://dsw.inu.ac.kr/dsw/2489/subview.do", "https://dsw.inu.ac.kr/dsw/2493/subview.do", "https://www.inu.ac.kr/bbs/isis/322/rssList.do?row=10", true, true, true),
    MEDIA_COMMUNICATION("미디어커뮤니케이션학과", "0000794", College.COLLEGE_OF_SOCIAL_SCIENCES, "https://newdays.inu.ac.kr/shinbang/2534/subview.do", "https://newdays.inu.ac.kr/shinbang/2533/subview.do", "https://newdays.inu.ac.kr/shinbang/2541/subview.do", "https://www.inu.ac.kr/bbs/isis/1697/rssList.do?row=10", true, true, true),
    LIBRARY_INFO("문헌정보학과", "0000053", College.COLLEGE_OF_SOCIAL_SCIENCES, "https://cls.inu.ac.kr/cls/2444/subview.do", "https://cls.inu.ac.kr/cls/2443/subview.do", "https://cls.inu.ac.kr/cls/2448/subview.do", "https://www.inu.ac.kr/bbs/isis/319/rssList.do?row=10", true, true, true),
    CREATIVE_HRD("창의인재개발학과", "0000054", College.COLLEGE_OF_SOCIAL_SCIENCES, "https://hrd.inu.ac.kr/hrd/2577/subview.do", "https://hrd.inu.ac.kr/hrd/2576/subview.do", "https://hrd.inu.ac.kr/hrd/2580/subview.do", "https://www.inu.ac.kr/bbs/isis/328/rssList.do?row=10", true, true, true),

    // 글로벌정경대학(5개)
    PUBLIC_ADMINISTRATION("행정학과", "0000698", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE, "https://uipa.inu.ac.kr/uipa/7797/subview.do", "https://uipa.inu.ac.kr/uipa/7796/subview.do", "https://uipa.inu.ac.kr/uipa/7800/subview.do", "https://www.inu.ac.kr/bbs/isis/1707/rssList.do?row=10", true, true, true),
    POLITICS_DIPLOMACY("정치외교학과", "0000699", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE, "https://politics.inu.ac.kr/politics/2739/subview.do", "https://politics.inu.ac.kr/politics/2738/subview.do", "https://politics.inu.ac.kr/politics/2742/subview.do", "https://www.inu.ac.kr/bbs/isis/337/rssList.do?row=10", true, true, true),
    ECONOMICS("경제학과", "0000700", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE, "https://econ.inu.ac.kr/econ/2638/subview.do", "https://econ.inu.ac.kr/econ/2637/subview.do", "https://econ.inu.ac.kr/econ/2643/subview.do", "https://www.inu.ac.kr/bbs/isis/331/rssList.do?row=10", true, true, true),
    TRADE("Global Trade & Service학부", "0000913", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE, "https://trade.inu.ac.kr/trade/2701/subview.do", "https://trade.inu.ac.kr/trade/2686/subview.do", "https://trade.inu.ac.kr/trade/2693/subview.do", "https://www.inu.ac.kr/bbs/isis/334/rssList.do?row=10", true, true, true),
    CONSUMER_SCIENCE("소비자학과", "0000704", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE, "https://ccs.inu.ac.kr/ccs/2809/subview.do", "https://ccs.inu.ac.kr/ccs/2807/subview.do", "https://ccs.inu.ac.kr/ccs/2789/subview.do", "https://www.inu.ac.kr/bbs/isis/340/rssList.do?row=10", true, false, true),

    // 공과대학(8개)
    ENERGY_CHEMICAL("에너지화학공학과", "0000055", College.COLLEGE_OF_ENGINEERING, null, "https://energy.inu.ac.kr/energy/3254/subview.do", "https://energy.inu.ac.kr/energy/3270/subview.do", "https://www.inu.ac.kr/bbs/isis/868/rssList.do?row=10", true, true, true),
    ELECTRICAL_ENGINEERING("전기공학과", "EPB1", College.COLLEGE_OF_ENGINEERING, "https://elec.inu.ac.kr/elec/3317/subview.do", "https://elec.inu.ac.kr/elec/3316/subview.do", "https://elec.inu.ac.kr/elec/3324/subview.do", "https://www.inu.ac.kr/bbs/isis/364/rssList.do?row=10", true, true, true),
    ELECTRONICS_ENGINEERING("전자공학부", "0000813", College.COLLEGE_OF_ENGINEERING, "https://ee.inu.ac.kr/electron/12184/subview.do", "https://ee.inu.ac.kr/electron/12199/subview.do", "https://ee.inu.ac.kr/electron/3376/subview.do", "https://www.inu.ac.kr/bbs/isis/367/rssList.do?row=10", true, false, true),
    INDUSTRIAL_MANAGEMENT("산업경영공학과", "EPG1", College.COLLEGE_OF_ENGINEERING, "https://ime.inu.ac.kr/ime/3094/subview.do", "https://ime.inu.ac.kr/ime/3093/subview.do", "https://ime.inu.ac.kr/ime/3101/subview.do", "https://www.inu.ac.kr/bbs/isis/826/rssList.do?row=10", true, true, false),
    MATERIAL_SCIENCE("신소재공학과", "0000076", College.COLLEGE_OF_ENGINEERING, "https://mse.inu.ac.kr/mse/3139/subview.do", "https://mse.inu.ac.kr/mse/3137/subview.do", "https://mse.inu.ac.kr/mse/3148/subview.do", "https://www.inu.ac.kr/bbs/isis/355/rssList.do?row=10", true, false, true),
    MECHANICAL_ENGINEERING("기계공학과", "0000459", College.COLLEGE_OF_ENGINEERING, "https://me.inu.ac.kr/me/12171/subview.do", "https://me.inu.ac.kr/me/2980/subview.do", "https://me.inu.ac.kr/me/2989/subview.do", "https://www.inu.ac.kr/bbs/isis/814/rssList.do?row=10", true, true, true),
    BIO_ROBOTICS_ENGINEERING("바이오-로봇시스템공학과", "0000814", College.COLLEGE_OF_ENGINEERING, "https://bio-robot.inu.ac.kr/meca/3045/subview.do", "https://bio-robot.inu.ac.kr/meca/3045/subview.do", "https://bio-robot.inu.ac.kr/meca/3049/subview.do", "https://www.inu.ac.kr/bbs/isis/349/rssList.do?row=10", true, true, true),
    SAFETY_ENGINEERING("안전공학과", "0000075", College.COLLEGE_OF_ENGINEERING, "https://safety.inu.ac.kr/safety/12167/subview.do", "https://safety.inu.ac.kr/safety/3195/subview.do", "https://safety.inu.ac.kr/safety/3206/subview.do", "https://www.inu.ac.kr/bbs/isis/358/rssList.do?row=10", true, true, true),

    // 정보기술대학(3개)
    COMPUTER_ENGINEERING("컴퓨터공학부", "0000077", College.COLLEGE_OF_INFORMATION_TECHNOLOGY, "https://cse.inu.ac.kr/isis/12172/subview.do", "https://cse.inu.ac.kr/isis/3521/subview.do", "https://cse.inu.ac.kr/isis/3519/subview.do", "https://www.inu.ac.kr/bbs/isis/376/rssList.do?row=10", true, true, true),
    INFORMATION_COMMUNICATION_ENGINEERING("정보통신공학과", "IAB1", College.COLLEGE_OF_INFORMATION_TECHNOLOGY, "https://ite.inu.ac.kr/ite/3467/subview.do", "https://ite.inu.ac.kr/ite/3466/subview.do", "https://ite.inu.ac.kr/ite/3472/subview.do", "https://www.inu.ac.kr/bbs/isis/373/rssList.do?row=10", true, true, true),
    EMBEDDED_SYSTEM("임베디드시스템공학과", "0000042", College.COLLEGE_OF_INFORMATION_TECHNOLOGY, "https://ese.inu.ac.kr/ese/3422/subview.do", "https://ese.inu.ac.kr/ese/3421/subview.do", "https://ese.inu.ac.kr/ese/3428/subview.do", "https://www.inu.ac.kr/bbs/isis/370/rssList.do?row=10", true, true, true),

    // 경영대학(3개)
    BUSINESS_ADMINISTRATION("경영학부", "JA01", College.COLLEGE_OF_BUSINESS_ADMINISTRATION, "https://biz.inu.ac.kr/biz/3605/subview.do", "https://biz.inu.ac.kr/biz/3604/subview.do", "https://biz.inu.ac.kr/biz/3612/subview.do", "https://www.inu.ac.kr/bbs/isis/379/rssList.do?row=10", true, true, true),
    DATA_SCIENCE("데이터과학과", "0000812", College.COLLEGE_OF_BUSINESS_ADMINISTRATION, "https://datascience.inu.ac.kr/datascience/3708/subview.do", "https://datascience.inu.ac.kr/datascience/3707/subview.do", "https://datascience.inu.ac.kr/datascience/3713/subview.do", "https://www.inu.ac.kr/bbs/isis/1825/rssList.do?row=10", false, true, true),
    TAX_ACCOUNTING("세무회계학과", "0000057", College.COLLEGE_OF_BUSINESS_ADMINISTRATION, "https://tax.inu.ac.kr/tax/3658/subview.do", "https://tax.inu.ac.kr/tax/3657/subview.do", "https://tax.inu.ac.kr/tax/3665/subview.do", "https://www.inu.ac.kr/bbs/isis/384/rssList.do?row=10", true, true, true),
    TECHNO_MANAGEMENT("계약학과", null, College.COLLEGE_OF_BUSINESS_ADMINISTRATION, null, null, "https://www.inu.ac.kr/contract/11207/subview.do", "https://www.inu.ac.kr/bbs/isis/2790/rssList.do?row=10", true, true, true),

    // 예술체육대학(7개)
    FINE_ARTS("조형예술학부", "0000192", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION, null, "https://finearts.inu.ac.kr/finearts/11426/subview.do", "https://finearts.inu.ac.kr/finearts/4130/subview.do", "https://www.inu.ac.kr/bbs/isis/409/rssList.do?row=10", true, true, true),
    KOREAN_PAINTING("한국화전공", "0000193", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION, null, "https://finearts.inu.ac.kr/finearts/4152/subview.do", null, null, false, false, false),
    WESTERN_PAINTING("서양화전공", "0000194", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION, null, "https://finearts.inu.ac.kr/finearts/4153/subview.do", null, null, false, false, false),
    DESIGN("디자인학부", "0000195", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION, "https://design.inu.ac.kr/design/4010/subview.do", "https://design.inu.ac.kr/design/4009/subview.do", "https://design.inu.ac.kr/design/4016/subview.do", "https://www.inu.ac.kr/bbs/isis/1842/rssList.do?row=10", false, true, false),
    PERFORMING_ART("공연예술학과", "0000196", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION, "https://uipa10.inu.ac.kr/uipa10/3951/subview.do", "https://uipa10.inu.ac.kr/uipa10/3950/subview.do", "https://uipa10.inu.ac.kr/uipa10/3957/subview.do", "https://www.inu.ac.kr/bbs/isis/400/rssList.do?row=10", true, true, true),
    SPORTS_SCIENCE("스포츠과학부", "0000815", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION, "https://sports.inu.ac.kr/sub3_2.php", "https://sports.inu.ac.kr/sub3_2.php", "https://sports.inu.ac.kr/bbs/board.php?bo_table=sub5_1", null, false, true, true),
    HEALTH_EXERCISE("운동건강학부", "0000191", College.COLLEGE_OF_ARTS_AND_PHYSICAL_EDUCATION, "https://uiex.inu.ac.kr/uiex/4060/subview.do", "https://uiex.inu.ac.kr/uiex/4059/subview.do", "https://uiex.inu.ac.kr/uiex/4068/subview.do", "https://www.inu.ac.kr/bbs/isis/406/rssList.do?row=10", true, true, true),

    // 사범대학(8개)
    KOREAN_EDUCATION("국어교육과", "0000064", College.COLLEGE_OF_EDUCATION, "https://edukorean.inu.ac.kr/edukorean/4238/subview.do", "https://edukorean.inu.ac.kr/edukorean/4237/subview.do", "https://edukorean.inu.ac.kr/edukorean/4262/subview.do", "https://www.inu.ac.kr/bbs/isis/1074/rssList.do?row=10", true, true, true),
    ENGLISH_EDUCATION("영어교육과", "0000065", College.COLLEGE_OF_EDUCATION, "https://eduenglish.inu.ac.kr/eduenglish/4412/subview.do", "https://eduenglish.inu.ac.kr/eduenglish/4411/subview.do", "https://eduenglish.inu.ac.kr/eduenglish/4436/subview.do", "https://www.inu.ac.kr/bbs/isis/1123/rssList.do?row=10", true, true, true),
    JAPANESE_EDUCATION("일어교육과", "0000066", College.COLLEGE_OF_EDUCATION, "https://edujapanese.inu.ac.kr/edujapanese/4598/subview.do", "https://edujapanese.inu.ac.kr/edujapanese/4597/subview.do", "https://edujapanese.inu.ac.kr/edujapanese/4609/subview.do", "https://www.inu.ac.kr/bbs/isis/1176/rssList.do?row=10", true, true, true),
    MATH_EDUCATION("수학교육과", "0000067", College.COLLEGE_OF_EDUCATION, "https://mathedu.inu.ac.kr/edumath/4301/subview.do", "https://mathedu.inu.ac.kr/edumath/4300/subview.do", "https://mathedu.inu.ac.kr/edumath/4319/subview.do", "https://www.inu.ac.kr/bbs/isis/1088/rssList.do?row=10", true, true, true),
    PHYSICAL_EDUCATION("체육교육과", "0000068", College.COLLEGE_OF_EDUCATION, "https://eduphysical.inu.ac.kr/eduphysical/4647/subview.do", "https://eduphysical.inu.ac.kr/eduphysical/4646/subview.do", "https://eduphysical.inu.ac.kr/eduphysical/4650/subview.do", null, false, true, true),
    EARLY_CHILDHOOD_EDUCATION("유아교육과", "0000069", College.COLLEGE_OF_EDUCATION, "https://ece.inu.ac.kr/ece/4477/subview.do", "https://ece.inu.ac.kr/ece/4477/subview.do", "https://ece.inu.ac.kr/ece/4492/subview.do", "https://www.inu.ac.kr/bbs/isis/1143/rssList.do?row=10", true, true, true),
    HISTORY_EDUCATION("역사교육과", "0000070", College.COLLEGE_OF_EDUCATION, "https://eduhistory.inu.ac.kr/eduhistory/7990/subview.do", "https://eduhistory.inu.ac.kr/eduhistory/7989/subview.do", "https://eduhistory.inu.ac.kr/eduhistory/8001/subview.do", "https://www.inu.ac.kr/bbs/isis/1104/rssList.do?row=10", true, true, true),
    ETHICS_EDUCATION("윤리교육과", "0000071", College.COLLEGE_OF_EDUCATION, "https://eduethics.inu.ac.kr/eduethics/4535/subview.do", "https://eduethics.inu.ac.kr/eduethics/4534/subview.do", "https://eduethics.inu.ac.kr/eduethics/4546/subview.do", "https://www.inu.ac.kr/bbs/isis/1161/rssList.do?row=10", true, true, true),

    // 도시과학대학(6개)
    URBAN_ADMINISTRATION("도시행정학과", "0000073", College.COLLEGE_OF_URBAN_SCIENCE, "https://urban.inu.ac.kr/urban/4885/subview.do", "https://urban.inu.ac.kr/urban/4884/subview.do", "https://urban.inu.ac.kr/urban/4920/subview.do", "https://www.inu.ac.kr/bbs/isis/1213/rssList.do?row=10", true, true, true),
    CIVIL_ENVIRONMENT_ENGINEERING("건설환경공학전공", "0000156", College.COLLEGE_OF_URBAN_SCIENCE, "https://civil.inu.ac.kr/civil/4705/subview.do", "https://civil.inu.ac.kr/civil/4704/subview.do", "https://civil.inu.ac.kr/civil/4707/subview.do", "https://www.inu.ac.kr/bbs/isis/1237/rssList.do?row=10", true, true, true),
    ENVIRONMENT_ENGINEERING("환경공학전공", "0000157", College.COLLEGE_OF_URBAN_SCIENCE, "https://et.inu.ac.kr/et/7721/subview.do", "https://et.inu.ac.kr/et/7720/subview.do", "https://et.inu.ac.kr/et/7728/subview.do", "https://www.inu.ac.kr/bbs/isis/1267/rssList.do?row=10", true, true, true),
    URBAN_ENGINEERING("도시공학과", "0000463", College.COLLEGE_OF_URBAN_SCIENCE, "https://scity.inu.ac.kr/ucv/4747/subview.do", "https://scity.inu.ac.kr/ucv/4746/subview.do", "https://scity.inu.ac.kr/ucv/4750/subview.do", "https://www.inu.ac.kr/bbs/isis/1252/rssList.do?row=10", true, true, true),
    URBAN_ARCHITECTURE("도시건축학부", "0000038", College.COLLEGE_OF_URBAN_SCIENCE, null, null, "https://archi.inu.ac.kr/archi/4818/subview.do", "https://www.inu.ac.kr/bbs/isis/1205/rssList.do?row=10", true, true, true),
    URBAN_ARCHITECTURE_ENGINEERING("건축공학전공", "0000160", College.COLLEGE_OF_URBAN_SCIENCE, "https://archi.inu.ac.kr/archi/4841/subview.do", "https://archi.inu.ac.kr/archi/4839/subview.do", null, null, false, false, false),
    URBAN_ARCHITECTURE_ARCHITECTURE("도시건축학전공", "0000464", College.COLLEGE_OF_URBAN_SCIENCE, "https://archi.inu.ac.kr/archi/4842/subview.do", "https://archi.inu.ac.kr/archi/4840/subview.do", null, null, false, false, false),

    // 생명과학기술대학(4개)
    LIFE_SCIENCE("생명과학전공", "0000184", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY, "https://life.inu.ac.kr/life/4961/subview.do", "https://life.inu.ac.kr/life/4960/subview.do", "https://life.inu.ac.kr/life/4954/subview.do", "https://www.inu.ac.kr/bbs/isis/1290/rssList.do?row=10", true, true, true),
    LIFE_SCIENCE_MOLECULAR("분자의생명전공", "0000185", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY, "https://molbio.inu.ac.kr/molbio/5005/subview.do", "https://molbio.inu.ac.kr/molbio/5004/subview.do", "https://molbio.inu.ac.kr/molbio/4999/subview.do", "https://www.inu.ac.kr/bbs/isis/1883/rssList.do?row=10", false, true, true),
    BIOENGINEERING("생명공학전공", "0000187", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY, "https://bioeng.inu.ac.kr/engineeringlife/5127/subview.do", "https://bioeng.inu.ac.kr/engineeringlife/5126/subview.do", "https://bioeng.inu.ac.kr/engineeringlife/5121/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGZW5naW5lZXJpbmdsaWZlJTJGMTI4NSUyRmFydGNsTGlzdC5kbyUzRmJic0NsU2VxJTNEMTYxNiUyNmJic09wZW5XcmRTZXElM0QlMjZpc1ZpZXdNaW5lJTNEZmFsc2UlMjZzcmNoQ29sdW1uJTNEc2olMjZzcmNoV3JkJTNEJTI2", "https://www.inu.ac.kr/bbs/isis/1285/rssList.do?row=10", true, true, true),
    BIOENGINEERING_NANO("나노바이오공학전공", "0000833", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY, "https://www.inu.ac.kr/nanobio/12168/subview.do", "https://www.inu.ac.kr/nanobio/12157/subview.do", "https://nanobio.inu.ac.kr/nanobio/5070/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGbmFub2JpbyUyRjEyNzklMkZhcnRjbExpc3QuZG8lM0ZiYnNDbFNlcSUzRDE2MTIlMjZiYnNPcGVuV3JkU2VxJTNEJTI2aXNWaWV3TWluZSUzRGZhbHNlJTI2c3JjaENvbHVtbiUzRHNqJTI2c3JjaFdyZCUzRCUyNg%3D%3D", "https://www.inu.ac.kr/bbs/isis/1279/rssList.do?row=10", true, true, true),

    // 융합자유전공대학(1개)
    LIBERAL_ARTS("자유전공학부", "0000838", College.COLLEGE_OF_INTERDISCIPLINARY_STUDIES, null, null, "https://www.inu.ac.kr/clis/12702/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGY2xpcyUyRjI5NjklMkZhcnRjbExpc3QuZG8lM0ZiYnNDbFNlcSUzRDE5OTMlMjZiYnNPcGVuV3JkU2VxJTNEJTI2aXNWaWV3TWluZSUzRGZhbHNlJTI2c3JjaENvbHVtbiUzRHNqJTI2c3JjaFdyZCUzRCUyNg%3D%3D", "https://www.inu.ac.kr/bbs/isis/2969/rssList.do?row=10", false, true, true),
    INTERNATIONAL_LIBERAL_ARTS("국제자유전공학부", null, College.COLLEGE_OF_INTERDISCIPLINARY_STUDIES, null, null, "https://www.inu.ac.kr/clis/12702/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGY2xpcyUyRjI5NjklMkZhcnRjbExpc3QuZG8lM0ZiYnNDbFNlcSUzRDE5OTUlMjZiYnNPcGVuV3JkU2VxJTNEJTI2aXNWaWV3TWluZSUzRGZhbHNlJTI2c3JjaENvbHVtbiUzRHNqJTI2c3JjaFdyZCUzRCUyNg%3D%3D", "https://www.inu.ac.kr/bbs/isis/2969/rssList.do?row=10", false, true, true),
    CONVERGENCE("융합학부", null, College.COLLEGE_OF_INTERDISCIPLINARY_STUDIES, null, null, "https://www.inu.ac.kr/clis/12702/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGY2xpcyUyRjI5NjklMkZhcnRjbExpc3QuZG8lM0ZiYnNDbFNlcSUzRDE5OTYlMjZiYnNPcGVuV3JkU2VxJTNEJTI2aXNWaWV3TWluZSUzRGZhbHNlJTI2c3JjaENvbHVtbiUzRHNqJTI2c3JjaFdyZCUzRCUyNg%3D%3D", "https://www.inu.ac.kr/bbs/isis/2969/rssList.do?row=10", false, true, true),

    // 단과대 없음(4개)
    // 동북아국제통상물류학부(3개) / 법학부(1개)
    NORTHEAST_ASIAN_TRADE("동북아국제통상전공", "0000817", College.NO_COLLEGE, "https://www.inu.ac.kr/nas/3792/subview.do", null, "https://www.inu.ac.kr/nas/3798/subview.do", "https://www.inu.ac.kr/bbs/isis/1830/rssList.do?row=10", false, true, true),
    SMART_LOGISTICS_ENGINEERING("스마트물류공학전공", "0000818", College.NO_COLLEGE, "https://slog.inu.ac.kr/slog/3837/subview.do", "https://slog.inu.ac.kr/slog/3836/subview.do", "https://slog.inu.ac.kr/slog/3842/subview.do", "https://www.inu.ac.kr/bbs/isis/1833/rssList.do?row=10", false, true, true),
    IBE("IBE전공", "0000832", College.NO_COLLEGE, null, null, "https://ibe.inu.ac.kr/ibe/3887/subview.do", "https://www.inu.ac.kr/bbs/isis/1840/rssList.do?row=10", false, true, true),
    LAW("법학부", "0000707", College.LAW_NO_COLLEGE, "https://law.inu.ac.kr/law/5177/subview.do", "https://law.inu.ac.kr/law/5176/subview.do", "https://law.inu.ac.kr/law/5184/subview.do", "https://www.inu.ac.kr/bbs/isis/1299/rssList.do?row=10", true, true, true),


    /// 학과 외 데이터
    GENERAL("교양", "XAA0", College.GENERAL, null, null, null, null, false, false, false),
    GENERAL_ELECTIVE("일선", "WAA0", College.GENERAL_ELECTIVE, null, null, null, null, false, false, false),
    TEACHING("교직", "YAA0", College.TEACHING, null, null, null, null, false, false, false),
    MILITARY_SCIENCE("군사학", "ZAA0", College.MILITARY_SCIENCE, null, null, null, null, false, false, false),

    HUSS_OTHER_UNIVERSITY("HUSS(타대학)", "VEA1", College.ETC, null, null, null, null, false, false, false),
    HUSS_INCLUSIVE_SOCIAL_INITIATIVE("HUSS포용사회이니셔티브학부", "VE00", College.ETC, null, null, null, null, false, false, false),
    OPTICAL_ELECTRONICS_LINKED("광전자공학전공(연계)", "VAB1", College.ETC, null, null, null, null, false, false, false),
    LOGISTICS_LINKED("물류학전공(연계)", "VAC1", College.ETC, null, null, null, null, false, false, false),
    FUTURE_EDUCATION_DESIGN_LINKED("미래교육디자인연계전공", "0000849", College.ETC, null, null, null, null, false, false, false),
    FUTURE_CAR_LINKED("미래자동차연계전공", "0000789", College.ETC, null, null, null, null, false, false, false),
    SEMICONDUCTOR_CONVERGENCE("반도체융합전공", "0000829", College.ETC, null, null, null, null, false, false, false),
    SOCIAL_DATA_SCIENCE_LINKED("소셜데이터사이언스연계전공", "0000678", College.ETC, null, null, null, null, false, false, false),
    HUMANITIES_CULTURE_ART_PLANNING_LINKED("인문문화예술기획연계전공", "0000677", College.ETC, null, null, null, null, false, false, false),
    INTELLIGENT_ROBOT_SYSTEM_LINKED("지능형로봇시스템연계전공", "0000912", College.ETC, null, null, null, null, false, false, false),
    CREATIVE_DESIGN_LINKED("창의적디자인연계전공", "0000616", College.ETC, null, null, null, null, false, false, false),
    INTERNATIONAL_DEVELOPMENT_COOPERATION_LINKED("국제개발협력연계전공", null, College.ETC, null, null, null, null, false, false, false),

    // 학교 API(course_info) 에만 존재하는 야간·통합학부·구 학과 코드.
    // API DEPT_CODE 를 그대로 상수화한다. (개별 공지 페이지가 없어 공지 서비스는 미제공)
    ECONOMICS_EVENING("경제학과(야)", "0000701", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE, null, null),
    TRADE_EVENING("무역학부(야)", "0000703", College.COLLEGE_OF_GLOBAL_ECONOMICS_AND_TRADE, null, null),
    ELECTRONICS_ENGINEERING_DEPARTMENT("전자공학과", "EPC1", College.COLLEGE_OF_ENGINEERING, null, null),
    ELECTRONICS_ENGINEERING_MAJOR("전자공학전공", "0000828", College.COLLEGE_OF_ENGINEERING, null, null),
    URBAN_ENVIRONMENT_DIVISION("도시환경공학부", "0000034", College.COLLEGE_OF_URBAN_SCIENCE, null, null),
    LIFE_SCIENCE_DIVISION("생명과학부", "0000183", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY, null, null),
    BIOENGINEERING_DIVISION("생명공학부", "0000186", College.COLLEGE_OF_LIFE_SCIENCES_AND_BIOTECHNOLOGY, null, null),

    UNKNOWN("알 수 없는 값", null, College.UNKNOWN, null, null, null, null, false, false, false);


    private final String departmentName;    // 학과명 (학교 API DEPT_NAME 과 동일)
    private final String departmentCode;    // 학과코드
    private final College collegeName;      // 단과대명
    private final String courseOverviewUrl; // 교과목개요 Url
    private final String curriculumUrl;     // 교육과정 Url
    private final String noticeUrl;         // 공지사항 URL
    private final String rssUrl;            // RSS 피드 URL
    private final boolean rssSupported;     // rssUrl 기반 목록 수집을 지원 여부
    private final boolean serviceAvailable; // 학과 공지 서비스를 앱에서 제공 여부
    private final boolean contentAvailable; // 공지 상세 본문까지 가져올 수 있는지 여부

    // course 정보만 있는 경우
    Department(String departmentName, String departmentCode, College collegeName, String courseOverviewUrl, String curriculumUrl
    ) {
        this(departmentName, departmentCode, collegeName, courseOverviewUrl, curriculumUrl, null, null, false, false, false);
    }

    // notice까지 있고, 기본적으로 모두 지원하는 경우
    Department(String departmentName, String departmentCode, College collegeName, String courseOverviewUrl, String curriculumUrl, String noticeUrl, String rssUrl
    ) {
        this(departmentName, departmentCode, collegeName, courseOverviewUrl, curriculumUrl, noticeUrl, rssUrl, true, true, true);
    }

    // notice 지원 여부를 직접 지정해야 하는 예외 케이스
    Department(
            String departmentName,
            String departmentCode,
            College collegeName,
            String courseOverviewUrl,
            String curriculumUrl,
            String noticeUrl,
            String rssUrl,
            boolean rssSupported,
            boolean serviceAvailable,
            boolean contentAvailable
    ) {
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.collegeName = collegeName;
        this.courseOverviewUrl = courseOverviewUrl;
        this.curriculumUrl = curriculumUrl;
        this.noticeUrl = noticeUrl;
        this.rssUrl = rssUrl;
        this.rssSupported = rssSupported;
        this.serviceAvailable = serviceAvailable;
        this.contentAvailable = contentAvailable;
    }

    /**
     * 학과코드로 조회한다. 코드가 없거나(null/blank) 매칭되는 학과가 없으면 null.
     * 이 enum 의 departmentCode 는 학교 API(course_info) 의 DEPT_CODE 를 그대로 담고 있어
     * API 응답의 모든 DEPT_CODE 는 정확히 하나의 상수로 매핑된다.
     */
    public static Department fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        String trimmedCode = code.trim();
        for (Department department : values()) {
            if (trimmedCode.equals(department.departmentCode)) {
                return department;
            }
        }

        return null;
    }

    /**
     * 이 학과의 학교 API DEPT_CODE. (현재 코드-상수 1:1 이므로 대표 코드 하나, 없으면 빈 집합)
     * 개설강의를 학과 기준으로 필터링할 때 사용한다.
     */
    public java.util.Set<String> apiCodes() {
        return departmentCode == null ? java.util.Set.of() : java.util.Set.of(departmentCode);
    }

    /**
     * 학교 API 응답 매핑용. 학과코드 우선, 코드가 없을 때만(과거 편람 엑셀 등) 학과명으로 조회한다.
     * 어디에도 매칭되지 않으면 예외 대신 {@link #UNKNOWN} 을 반환한다.
     * (원본 문자열은 CourseOffering.deptNameRaw 등에 별도로 보존된다.)
     */
    public static Department fromApi(String code, String name) {
        Department byCode = fromCode(code);
        if (byCode != null) {
            return byCode;
        }

        if (name != null && !name.isBlank()) {
            String trimmedName = name.trim();
            for (Department department : values()) {
                if (department.name().equalsIgnoreCase(trimmedName)
                        || department.departmentName.equals(trimmedName)
                        || Objects.equals(department.departmentCode, trimmedName)) {
                    return department;
                }
            }
        }

        return UNKNOWN;
    }

    /**
     * 이름/코드로 조회하되, 매칭되지 않으면 예외를 던진다.
     * (프론트에서 넘어온 필터/경로 값 검증처럼 "잘못된 값 = 400" 이어야 하는 곳에서 사용)
     */
    public static Department from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        Department department = fromApi(null, value);
        if (department == UNKNOWN) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        return department;
    }

    // 공지 크롤링 가능한 department만 선택해주는 메서드(noticeService에서 사용)
    public static Department[] noticeDepartments() {
        return Arrays.stream(values())
                .filter(Department::isServiceAvailable)
                .toArray(Department[]::new);
    }

    // 기존 notice 코드 호환용
    public String getUrls() {
        return noticeUrl;
    }

}
