package kr.inuappcenterportal.inuportal.domain.agent.openapi.dto;

import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Method;

@Getter
@Builder
public class OpenApiToolDescriptor {
    private final String name;
    private final String description;
    private final Object bean;
    private final Method method;
    private final String redirectUrl;
    private final String cardTitle;
}
