package kr.inuappcenterportal.inuportal.domain.agent.openapi.service;

import kr.inuappcenterportal.inuportal.domain.agent.openapi.annotation.AgentExposed;
import kr.inuappcenterportal.inuportal.domain.agent.openapi.dto.OpenApiToolDescriptor;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolDefinition;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolParameter;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolRegistry;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiToolRegistry implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;
    private final AgentToolRegistry agentToolRegistry;
    private final InProcessApiDispatcher dispatcher;
    private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Override
    public void afterSingletonsInstantiated() {
        log.info("[OpenApiToolRegistry] 컨트롤러 엔드포인트 자동 탐색(Introspection) 및 AI 도구 등록을 시작합니다...");

        Map<String, Object> controllerBeans = applicationContext.getBeansWithAnnotation(RestController.class);
        controllerBeans.putAll(applicationContext.getBeansWithAnnotation(Controller.class));

        int exposedCount = 0;

        for (Object bean : controllerBeans.values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                AgentExposed exposed = AnnotationUtils.findAnnotation(method, AgentExposed.class);
                if (exposed == null) {
                    continue;
                }

                // [가드레일 1: 사이드이펙트 격리] 오직 GET 메서드만 등록 허용
                if (!isSafeGetMethod(method)) {
                    log.error("[OpenApiToolRegistry] 사이드이펙트 위험 감지: {}#{} 메서드는 읽기 전용(GET)이 아니므로 AI 도구 노출에서 원천 차단합니다.",
                            clazz.getSimpleName(), method.getName());
                    continue;
                }

                String toolName = "API_" + exposed.name().toUpperCase().trim();
                OpenApiToolDescriptor descriptor = OpenApiToolDescriptor.builder()
                        .name(toolName)
                        .description(exposed.description())
                        .bean(bean)
                        .method(method)
                        .redirectUrl(exposed.redirectUrl())
                        .cardTitle(exposed.cardTitle())
                        .build();

                // AgentTool 어댑터 생성 및 등록
                AgentTool adapter = new AgentTool() {
                    @Override
                    public AgentToolDefinition getDefinition() {
                        return new AgentToolDefinition(
                                descriptor.getName(), descriptor.getDescription(),
                                java.util.List.of(exposed.capabilities()), java.util.List.of(exposed.triggerExamples()),
                                java.util.List.of(exposed.negativeExamples()), inferParameters(method),
                                exposed.requiresLogin(), true
                        );
                    }

                    @Override
                    public ToolResult execute(Member member, Map<String, Object> params) {
                        return dispatcher.dispatch(descriptor, member, params);
                    }
                };

                agentToolRegistry.registerDynamicTool(adapter);
                exposedCount++;
            }
        }

        log.info("[OpenApiToolRegistry] 총 {}개의 OpenAPI 엔드포인트가 AI 도구로 자동 등록되었습니다.", exposedCount);
    }

    private boolean isSafeGetMethod(Method method) {
        if (AnnotationUtils.findAnnotation(method, GetMapping.class) != null) {
            return true;
        }
        RequestMapping rm = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        if (rm != null) {
            for (RequestMethod m : rm.method()) {
                if (m == RequestMethod.GET) return true;
            }
        }
        // POST, PUT, DELETE, PATCH 등은 거부
        return false;
    }

    private Map<String, AgentToolParameter> inferParameters(Method method) {
        Map<String, AgentToolParameter> parameters = new java.util.LinkedHashMap<>();
        String[] discoveredNames = paramNameDiscoverer.getParameterNames(method);
        Parameter[] methodParams = method.getParameters();

        for (int i = 0; i < methodParams.length; i++) {
            Parameter parameter = methodParams[i];
            RequestParam requestParam = AnnotationUtils.findAnnotation(parameter, RequestParam.class);
            if (requestParam == null) continue;

            String discoveredName = (discoveredNames != null && discoveredNames.length > i)
                    ? discoveredNames[i]
                    : parameter.getName();

            String name = !requestParam.name().isBlank() ? requestParam.name()
                    : !requestParam.value().isBlank() ? requestParam.value() : discoveredName;

            // Swagger @Parameter 어노테이션에서 설명(description)과 예시(example) 추출
            io.swagger.v3.oas.annotations.Parameter swaggerParam =
                    AnnotationUtils.findAnnotation(parameter, io.swagger.v3.oas.annotations.Parameter.class);
            String desc = (swaggerParam != null && !swaggerParam.description().isBlank())
                    ? swaggerParam.description().trim()
                    : inferFriendlyDescription(name);

            Class<?> paramType = parameter.getType();
            boolean isList = java.util.List.class.isAssignableFrom(paramType) || paramType.isArray();
            boolean isInteger = paramType == int.class || paramType == Integer.class || paramType == long.class || paramType == Long.class;
            boolean isBool = paramType == boolean.class || paramType == Boolean.class;

            java.util.List<String> enumValues = new java.util.ArrayList<>();
            if (paramType.isEnum()) {
                for (Object constant : paramType.getEnumConstants()) {
                    enumValues.add(constant.toString());
                }
            }

            String defaultValue = requestParam.defaultValue();
            boolean hasDefault = defaultValue != null && !defaultValue.equals(org.springframework.web.bind.annotation.ValueConstants.DEFAULT_NONE);
            boolean isRequired = requestParam.required() && !hasDefault;
            if (swaggerParam != null && swaggerParam.required()) {
                isRequired = true;
            }

            AgentToolParameter spec;
            if (isList) {
                spec = AgentToolParameter.array(desc, isRequired, enumValues.toArray(new String[0]));
            } else if (isInteger) {
                spec = AgentToolParameter.integer(desc, isRequired);
            } else if (isBool) {
                spec = AgentToolParameter.bool(desc, isRequired);
            } else {
                spec = AgentToolParameter.string(desc, isRequired, enumValues.toArray(new String[0]));
            }
            parameters.put(name, spec);
        }
        return parameters;
    }

    private String inferFriendlyDescription(String name) {
        return switch (name) {
            case "year" -> "조회 연도 (예: 2026, 4자리 숫자)";
            case "term" -> "학기 구분 (FIRST: 1학기, SECOND: 2학기, SUMMER: 여름계절학기, WINTER: 겨울계절학기)";
            case "deptName" -> "공식 학과/학부명 (예: 컴퓨터공학부, 경영학부, 데이터과학과 등)";
            case "hyNames" -> "수강 대상 학년 필터 (예: ['1'], ['2'], ['3'], ['4'])";
            case "keyword", "q", "query" -> "검색 키워드";
            case "sort" -> "정렬 기준 (DEFAULT, SAVED_COUNT_DESC 등)";
            case "page" -> "조회 페이지 번호 (0부터 시작)";
            default -> name + " 파라미터";
        };
    }
}
