package kr.inuappcenterportal.inuportal.domain.agent.openapi.annotation;

import java.lang.annotation.*;

/**
 * AI 에이전트에게 안전하게 개방할 컨트롤러 메서드를 선언하는 어노테이션.
 * 
 * [가드레일 정책]
 * 1. 안전성 격리: 오직 부작용(Side-effect)이 없는 읽기 전용(@GetMapping) 메서드만 등록 가능합니다.
 * 2. 정보 은닉: 이 어노테이션이 붙지 않은 엔드포인트는 AI 도구 카탈로그에 노출되지 않습니다.
 * 3. 권한 바인딩: 요청 시 현재 로그인한 사용자의 Member 세션이 자동으로 전파됩니다.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentExposed {

    /**
     * AI 도구 고유 이름 (예: "LOST_PROPERTY", "CLUB_LIST", "COMMUNITY_POSTS")
     * 에이전트 레지스트리에는 "API_" 접두사가 붙어 등록됩니다.
     */
    String name();

    /** AI 라우팅용 한 줄 요약. 파라미터는 메서드 시그니처에서 자동 추론한다. */
    String description();

    /** 실제 엔드포인트가 수행하는 작업 목록. */
    String[] capabilities();

    /** 이 도구를 선택해야 하는 자연어 발화 예시. */
    String[] triggerExamples();

    /** 유사하지만 다른 도구를 선택하거나 지원하지 않는 발화 경계. */
    String[] negativeExamples() default {};

    /** 조회에 로그인 세션이 필요한지 여부. */
    boolean requiresLogin() default false;

    /**
     * 프론트엔드 이동 딥링크 URL (선택)
     */
    String redirectUrl() default "";

    /**
     * 결과 카드 타이틀 (선택)
     */
    String cardTitle() default "조회 결과";
}
