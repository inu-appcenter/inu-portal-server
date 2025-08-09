package kr.inuappcenterportal.inuportal.domain.notice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Department {

    // 총 64개 학과

    // 인문대학
    KOREAN("국어국문학과", "https://korean.inu.ac.kr/korean/1780/subview.do"),
    ENGLISH("영어영문학과", "https://english.inu.ac.kr/ui/7888/subview.do"),
    GERMAN("독어독문학과", "https://german.inu.ac.kr/german/1841/subview.do"),
    FRENCH("불어불문학과", "https://inufrance.inu.ac.kr/inufrance/1915/subview.do"),
    JAPANESE("일본지역문화학과", "https://unjapan.inu.ac.kr/unjapan/2025/subview.do"),
    CHINESE("중어중국학과", "https://inuchina.inu.ac.kr/inuchina/2096/subview.do"),

    // 자연과학대학
    MATHEMATICS("수학과", "https://math.inu.ac.kr/isu/2219/subview.do"),
    PHYSICS("물리학과", "https://physics.inu.ac.kr/physics/2155/subview.do"),
    CHEMISTRY("화학과", "https://chem.inu.ac.kr/chem/2389/subview.do"),
    FASHION("패션산업학과", "https://uifashion.inu.ac.kr/uifashion/2271/subview.do"),
    MARINE("해양학과", "https://marine.inu.ac.kr/marine/2323/subview.do"),

    // 사회과학대학
    SOCIAL_WELFARE("사회복지학과", "https://dsw.inu.ac.kr/dsw/2493/subview.do"),
    MEDIA_COMMUNICATION("미디어커뮤니케이션학과", "https://newdays.inu.ac.kr/shinbang/2541/subview.do"),
    LIBRARY_INFO("문헌정보학과", "https://cls.inu.ac.kr/cls/2448/subview.do"),
    CREATIVE_HRD("창의인재개발학과", "https://hrd.inu.ac.kr/hrd/2580/subview.do"),

    // 글로벌정경대학
    PUBLIC_ADMINISTRATION("행정학과", "https://uipa.inu.ac.kr/uipa/7800/subview.do"),
    POLITICS_DIPLOMACY("정치외교학과", "https://politics.inu.ac.kr/politics/2742/subview.do"),
    ECONOMICS("경제학과", "https://econ.inu.ac.kr/econ/2643/subview.do"),
    TRADE("무역학부", "https://trade.inu.ac.kr/trade/12389/subview.do"),
    CONSUMER_SCIENCE("소비자학과", "https://ccs.inu.ac.kr/ccs/2789/subview.do"),

    // 공과대학
    ENERGY_CHEMICAL("에너지화학공학과", "https://energy.inu.ac.kr/energy/3270/subview.do"),
    ELECTRICAL_ENGINEERING("전기공학과", "https://elec.inu.ac.kr/elec/3324/subview.do"),
    ELECTRONICS_ENGINEERING("전자공학과", "https://ee.inu.ac.kr/electron/3376/subview.do"),
    INDUSTRIAL_MANAGEMENT("산업경영공학과", "https://ime.inu.ac.kr/ime/3101/subview.do"),
    MATERIAL_SCIENCE("신소재공학과", "https://mse.inu.ac.kr/mse/3148/subview.do"),
    MECHANICAL_ENGINEERING("기계공학과", "https://me.inu.ac.kr/me/2989/subview.do"),
    BIO_ROBOTICS_ENGINEERING("바이오로봇시스템공학과", "https://bio-robot.inu.ac.kr/meca/3049/subview.do"),
    SAFETY_ENGINEERING("안전공학과", "https://safety.inu.ac.kr/safety/3206/subview.do"),

    // 정보기술대학
    COMPUTER_ENGINEERING("컴퓨터공학부", "https://cse.inu.ac.kr/isis/3519/subview.do"),
    INFORMATION_COMMUNICATION_ENGINEERING("정보통신공학과", "https://ite.inu.ac.kr/ite/3472/subview.do"),
    EMBEDDED_SYSTEM("임베디드시스템공학과", "https://ese.inu.ac.kr/ese/3428/subview.do"),

    // 경영대학
    BUSINESS_ADMINISTRATION("경영학부", "https://biz.inu.ac.kr/biz/3612/subview.do"),
    DATA_SCIENCE("데이터과학과", "https://datascience.inu.ac.kr/datascience/3713/subview.do"),
    TAX_ACCOUNTING("세무회계학과", "https://tax.inu.ac.kr/tax/3665/subview.do"),
    TECHNO_MANAGEMENT("테크노경영학과", "https://www.inu.ac.kr/contract/11456/subview.do"),

    // 예술체육대학
    FINE_ARTS("조형예술학부", "https://finearts.inu.ac.kr/finearts/4130/subview.do"),
    DESIGN("디자인학부", "https://design.inu.ac.kr/design/4016/subview.do"),
    PERFORMING_ART("공연예술학과", "https://uipa10.inu.ac.kr/uipa10/3957/subview.do"),
    SPORTS_SCIENCE("스포츠과학부", "https://sports.inu.ac.kr/bbs/board.php?bo_table=sub5_1"), // 다름
    HEALTH_EXERCISE("운동건강학부", "https://uiex.inu.ac.kr/uiex/4068/subview.do"),

    // 사범대학
    KOREAN_EDUCATION("국어교육과", "https://edukorean.inu.ac.kr/edukorean/4262/subview.do"),
    ENGLISH_EDUCATION("영어교육과", "https://eduenglish.inu.ac.kr/eduenglish/4436/subview.do"),
    JAPANESE_EDUCATION("일어교육과", "https://edujapanese.inu.ac.kr/edujapanese/4609/subview.do"),
    MATH_EDUCATION("수학교육과", "https://mathedu.inu.ac.kr/edumath/4319/subview.do"),
    PHYSICAL_EDUCATION("체육교육과", "https://eduphysical.inu.ac.kr/eduphysical/4650/subview.do"),
    EARLY_CHILDHOOD_EDUCATION("유아교육과", "https://ece.inu.ac.kr/ece/4492/subview.do"),
    HISTORY_EDUCATION("역사교육과", "https://eduhistory.inu.ac.kr/eduhistory/8001/subview.do"),
    ETHICS_EDUCATION("윤리교육과", "https://eduethics.inu.ac.kr/eduethics/4546/subview.do"),

    // 도시과학대학
    URBAN_ADMINISTRATION("도시행정학과", "https://urban.inu.ac.kr/urban/4920/subview.do"),
    CIVIL_ENVIRONMENT_ENGINEERING("도시환경학부 (건설환경공학전공)", "https://civil.inu.ac.kr/civil/4707/subview.do"),
    ENVIRONMENT_ENGINEERING("도시환경학부 (환경공학전공)", "https://et.inu.ac.kr/et/7728/subview.do"),
    URBAN_ENGINEERING("도시공학과", "https://scity.inu.ac.kr/ucv/4750/subview.do"),
    URBAN_ARCHITECTURE("도시건축학부", "https://archi.inu.ac.kr/archi/4818/subview.do"),

    // 생명과학기술대학
    LIFE_SCIENCE("생명과학부 (생명과학전공)", "https://life.inu.ac.kr/life/4954/subview.do"),
    LIFE_SCIENCE_MOLECULAR("생명과학부 (분자의생명전공)", "https://molbio.inu.ac.kr/molbio/4999/subview.do"),
    BIOENGINEERING("생명공학부 (생명공학전공)", "https://bioeng.inu.ac.kr/engineeringlife/5121/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGZW5naW5lZXJpbmdsaWZlJTJGMTI4NSUyRmFydGNsTGlzdC5kbyUzRmJic0NsU2VxJTNEMTYxNiUyNmJic09wZW5XcmRTZXElM0QlMjZpc1ZpZXdNaW5lJTNEZmFsc2UlMjZzcmNoQ29sdW1uJTNEc2olMjZzcmNoV3JkJTNEJTI2"),
    BIOENGINEERING_NANO("생명공학부 (나노바이오공학전공)", "https://nanobio.inu.ac.kr/nanobio/5070/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGbmFub2JpbyUyRjEyNzklMkZhcnRjbExpc3QuZG8lM0ZiYnNDbFNlcSUzRDE2MTIlMjZiYnNPcGVuV3JkU2VxJTNEJTI2aXNWaWV3TWluZSUzRGZhbHNlJTI2c3JjaENvbHVtbiUzRHNqJTI2c3JjaFdyZCUzRCUyNg%3D%3D"),

    // 융합자유전공대학
    LIBERAL_ARTS("자유전공학부", "https://www.inu.ac.kr/clis/12702/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGY2xpcyUyRjI5NjklMkZhcnRjbExpc3QuZG8lM0ZiYnNDbFNlcSUzRDE5OTMlMjZiYnNPcGVuV3JkU2VxJTNEJTI2aXNWaWV3TWluZSUzRGZhbHNlJTI2c3JjaENvbHVtbiUzRHNqJTI2c3JjaFdyZCUzRCUyNg%3D%3D"),
    INTERNATIONAL_LIBERAL_ARTS("국제자유전공학부", "https://www.inu.ac.kr/clis/12702/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGY2xpcyUyRjI5NjklMkZhcnRjbExpc3QuZG8lM0ZiYnNDbFNlcSUzRDE5OTUlMjZiYnNPcGVuV3JkU2VxJTNEJTI2aXNWaWV3TWluZSUzRGZhbHNlJTI2c3JjaENvbHVtbiUzRHNqJTI2c3JjaFdyZCUzRCUyNg%3D%3D"),
    CONVERGENCE("융합학부", "https://www.inu.ac.kr/clis/12702/subview.do?enc=Zm5jdDF8QEB8JTJGYmJzJTJGY2xpcyUyRjI5NjklMkZhcnRjbExpc3QuZG8lM0ZiYnNDbFNlcSUzRDE5OTYlMjZiYnNPcGVuV3JkU2VxJTNEJTI2aXNWaWV3TWluZSUzRGZhbHNlJTI2c3JjaENvbHVtbiUzRHNqJTI2c3JjaFdyZCUzRCUyNg%3D%3D"),

    // 동북아국제통상학부
    NORTHEAST_ASIAN_TRADE("동북아국제통상전공", "https://www.inu.ac.kr/nas/3798/subview.do"),
    SMART_LOGISTICS_ENGINEERING("스마트물류공학전공", "https://slog.inu.ac.kr/slog/3842/subview.do"),
    IBE("IBE전공", "https://ibe.inu.ac.kr/ibe/3887/subview.do"),

    // 법학부
    LAW("법학부", "https://law.inu.ac.kr/law/5184/subview.do");

    private final String departmentName;
    private final String urls;
}
