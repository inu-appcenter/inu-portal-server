package kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DEPT_NAME {
    GLOBAL_TRADE_SERVICE("Global Trade & Service학부"),
    HUSS_OTHER_UNIVERSITY("HUSS(타대학)"),
    HUSS_INCLUSIVE_SOCIETY_INITIATIVE("HUSS포용사회이니셔티브학부"),
    IBE("IBE전공"),

    CIVIL_ENVIRONMENTAL_ENGINEERING("건설환경공학전공"),
    ARCHITECTURAL_ENGINEERING("건축공학전공"),
    BUSINESS_ADMINISTRATION("경영학부"),
    ECONOMICS("경제학과"),
    ECONOMICS_EVENING("경제학과(야)"),
    PERFORMING_ARTS("공연예술학과"),
    OPTOELECTRONIC_ENGINEERING("광전자공학전공(연계)"),

    GENERAL_EDUCATION("교양"),
    TEACHING_PROFESSION("교직"),
    KOREAN_LANGUAGE_EDUCATION("국어교육과"),
    KOREAN_LANGUAGE_LITERATURE("국어국문학과"),
    MILITARY_SCIENCE("군사학"),
    MECHANICAL_ENGINEERING("기계공학과"),
    NANO_BIO_ENGINEERING("나노바이오공학전공"),
    DATA_SCIENCE("데이터과학과"),

    URBAN_ARCHITECTURE_DIVISION("도시건축학부"),
    URBAN_ARCHITECTURE("도시건축학전공"),
    URBAN_ENGINEERING("도시공학과"),
    URBAN_ADMINISTRATION("도시행정학과"),
    URBAN_ENVIRONMENTAL_ENGINEERING("도시환경공학부"),

    GERMAN_LANGUAGE_LITERATURE("독어독문학과"),
    NORTHEAST_ASIA_TRADE("동북아국제통상전공"),
    DESIGN("디자인학부"),
    INTERNATIONAL_TRADE_EVENING("무역학부(야)"),
    LIBRARY_INFORMATION_SCIENCE("문헌정보학과"),
    LOGISTICS("물류학전공(연계)"),
    PHYSICS("물리학과"),
    MEDIA_COMMUNICATION("미디어커뮤니케이션학과"),

    FUTURE_EDUCATION_DESIGN("미래교육디자인연계전공"),
    FUTURE_AUTOMOTIVE("미래자동차연계전공"),
    BIO_ROBOT_SYSTEM_ENGINEERING("바이오-로봇시스템공학과"),
    SEMICONDUCTOR_CONVERGENCE("반도체융합전공"),
    LAW("법학부"),
    MOLECULAR_MEDICAL_SCIENCE("분자의생명전공"),
    FRENCH_LANGUAGE_LITERATURE("불어불문학과"),
    SOCIAL_WELFARE("사회복지학과"),
    INDUSTRIAL_MANAGEMENT_ENGINEERING("산업경영공학과"),

    BIOENGINEERING_DIVISION("생명공학부"),
    BIOENGINEERING("생명공학전공"),
    BIOLOGICAL_SCIENCES_DIVISION("생명과학부"),
    BIOLOGICAL_SCIENCES("생명과학전공"),
    WESTERN_PAINTING("서양화전공"),
    TAX_ACCOUNTING("세무회계학과"),
    CONSUMER_SCIENCE("소비자학과"),
    SOCIAL_DATA_SCIENCE("소셜데이터사이언스연계전공"),

    MATHEMATICS("수학과"),
    MATHEMATICS_EDUCATION("수학교육과"),
    SMART_LOGISTICS_ENGINEERING("스마트물류공학전공"),
    SPORTS_SCIENCE("스포츠과학부"),
    MATERIALS_SCIENCE_ENGINEERING("신소재공학과"),
    SAFETY_ENGINEERING("안전공학과"),
    ENERGY_CHEMICAL_ENGINEERING("에너지화학공학과"),

    HISTORY_EDUCATION("역사교육과"),
    ENGLISH_EDUCATION("영어교육과"),
    ENGLISH_LANGUAGE_LITERATURE("영어영문학과"),
    EXERCISE_HEALTH_SCIENCE("운동건강학부"),
    EARLY_CHILDHOOD_EDUCATION("유아교육과"),
    ETHICS_EDUCATION("윤리교육과"),
    HUMANITIES_CULTURE_ARTS_PLANNING("인문문화예술기획연계전공"),

    JAPANESE_REGIONAL_CULTURE("일본지역문화학과"),
    GENERAL_ELECTIVE("일선"),
    JAPANESE_LANGUAGE_EDUCATION("일어교육과"),
    EMBEDDED_SYSTEM_ENGINEERING("임베디드시스템공학과"),
    OPEN_MAJOR("자유전공학부"),
    ELECTRICAL_ENGINEERING("전기공학과"),

    ELECTRONICS_ENGINEERING_DEPARTMENT("전자공학과"),
    ELECTRONICS_ENGINEERING_DIVISION("전자공학부"),
    ELECTRONICS_ENGINEERING_MAJOR("전자공학전공"),
    INFORMATION_TELECOMMUNICATION_ENGINEERING("정보통신공학과"),
    POLITICAL_SCIENCE_DIPLOMACY("정치외교학과"),
    FINE_ARTS("조형예술학부"),
    CHINESE_LANGUAGE_CHINA_STUDIES("중어중국학과"),

    INTELLIGENT_ROBOT_SYSTEM("지능형로봇시스템연계전공"),
    CREATIVE_HUMAN_RESOURCE_DEVELOPMENT("창의인재개발학과"),
    CREATIVE_DESIGN("창의적디자인연계전공"),
    PHYSICAL_EDUCATION("체육교육과"),
    COMPUTER_SCIENCE_ENGINEERING("컴퓨터공학부"),
    FASHION_INDUSTRY("패션산업학과"),

    KOREAN_PAINTING("한국화전공"),
    MARINE_SCIENCE("해양학과"),
    PUBLIC_ADMINISTRATION("행정학과"),
    CHEMISTRY("화학과"),
    ENVIRONMENTAL_ENGINEERING("환경공학전공");

    private final String description;
}
