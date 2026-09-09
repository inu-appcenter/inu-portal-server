package kr.inuappcenterportal.inuportal.domain.agent.openapi.service;

import kr.inuappcenterportal.inuportal.domain.agent.openapi.annotation.AgentExposed;
import kr.inuappcenterportal.inuportal.domain.agent.openapi.dto.OpenApiToolDescriptor;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentToolRegistry;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiToolRegistry implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;
    private final AgentToolRegistry agentToolRegistry;
    private final InProcessApiDispatcher dispatcher;

    @Override
    public void afterSingletonsInstantiated() {
        log.info("[OpenApiToolRegistry] 컨트롤러 엔드포인트 자동 탐색(Introspection) 및 AI 도구 등록을 시작합니다...");

        Map<String, Object> controllerBeans = applicationContext.getBeansWithAnnotation(RestController.class);
        controllerBeans.putAll(applicationContext.getBeansWithAnnotation(Controller.class));

        int exposedCount = 0;

        for (Object bean : controllerBeans.values()) {
            Class<?> clazz = bean.getClass();
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
                    public String getName() {
                        return descriptor.getName();
                    }

                    @Override
                    public String getDescription() {
                        return descriptor.getDescription();
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
}
