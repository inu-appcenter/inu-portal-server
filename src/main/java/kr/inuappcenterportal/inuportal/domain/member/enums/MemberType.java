package kr.inuappcenterportal.inuportal.domain.member.enums;

public enum MemberType {
    STUDENT,   // 학생 (학번이 9자리 숫자인 경우: ^\d{9}$) -> 학적 동기화 필수 대상
    FACULTY,   // 교직원 / 교수 (학번이 9자리 숫자 이외인 경우) -> 학적 동기화 제외
    FRESHMAN   // 신입생 (수험번호 기반 가입 유저) -> 3월 학번 발급 시 STUDENT로 승격
}
