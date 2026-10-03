package kr.inuappcenterportal.inuportal.domain.agent.openapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.openapi.dto.OpenApiToolDescriptor;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InProcessApiDispatcher {

    private final ObjectMapper objectMapper;
    private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();
    private static final int MAX_PREVIEW_ITEMS = 4;

    public AgentTool.ToolResult dispatch(OpenApiToolDescriptor descriptor, Member member, Map<String, Object> rawParams) {
        try {
            Method method = descriptor.getMethod();
            Object bean = descriptor.getBean();
            Parameter[] parameters = method.getParameters();
            String[] discoveredNames = paramNameDiscoverer.getParameterNames(method);

            Object[] args = new Object[parameters.length];
            Map<String, Object> lowerParams = new HashMap<>();
            if (rawParams != null) {
                for (Map.Entry<String, Object> e : rawParams.entrySet()) {
                    if (e.getKey() != null) {
                        lowerParams.put(e.getKey().toLowerCase(), e.getValue());
                    }
                }
            }

            for (int i = 0; i < parameters.length; i++) {
                Parameter p = parameters[i];
                Class<?> pType = p.getType();

                // 1. Member 주입
                if (Member.class.isAssignableFrom(pType)) {
                    args[i] = member;
                    continue;
                }

                String paramName = (discoveredNames != null && discoveredNames.length > i)
                        ? discoveredNames[i]
                        : p.getName();

                RequestParam rp = AnnotationUtils.getAnnotation(p, RequestParam.class);
                PathVariable pv = AnnotationUtils.getAnnotation(p, PathVariable.class);

                String key = paramName;
                String defaultValue = null;

                if (rp != null) {
                    if (!rp.name().isBlank()) key = rp.name();
                    else if (!rp.value().isBlank()) key = rp.value();
                    if (!ValueConstants.DEFAULT_NONE.equals(rp.defaultValue())) {
                        defaultValue = rp.defaultValue();
                    }
                } else if (pv != null) {
                    if (!pv.name().isBlank()) key = pv.name();
                    else if (!pv.value().isBlank()) key = pv.value();
                }

                Object rawVal = lowerParams.get(key.toLowerCase());
                if (rawVal == null && defaultValue != null) {
                    rawVal = defaultValue;
                }

                args[i] = convertValue(rawVal, pType);
            }

            method.setAccessible(true);
            Object rawResult = method.invoke(bean, args);

            if (rawResult instanceof ResponseEntity<?> entity) {
                rawResult = entity.getBody();
            }
            if (rawResult instanceof ResponseDto<?> respDto) {
                rawResult = respDto.getData();
            }

            // 가드레일: 결과 Truncation & 요약 생성
            List<Object> items = new ArrayList<>();
            long totalCount = 0;

            if (rawResult instanceof ListResponseDto<?> listResp) {
                totalCount = listResp.getTotal();
                if (listResp.getContents() != null) {
                    items.addAll(listResp.getContents());
                }
            } else if (rawResult instanceof Collection<?> coll) {
                totalCount = coll.size();
                items.addAll(coll);
            } else if (rawResult != null) {
                items.add(rawResult);
                totalCount = 1;
            }

            List<Object> truncatedItems = items.subList(0, Math.min(items.size(), MAX_PREVIEW_ITEMS));

            StringBuilder summary = new StringBuilder();
            summary.append(String.format("[%s] 조회 결과입니다 (총 %d건):\n", descriptor.getCardTitle(), totalCount));

            if (truncatedItems.isEmpty()) {
                summary.append("조회된 정보가 없습니다.");
            } else {
                for (Object item : truncatedItems) {
                    summary.append("• ").append(formatItemSummary(item)).append("\n");
                }
            }

            Map<String, Object> componentData = new LinkedHashMap<>();
            componentData.put("title", descriptor.getCardTitle());
            componentData.put("totalCount", totalCount);
            componentData.put("items", truncatedItems);
            componentData.put("redirectUrl", descriptor.getRedirectUrl());

            UiComponentDto component = UiComponentDto.of(
                    "DYNAMIC_DATA",
                    componentData,
                    descriptor.getCardTitle() + " 바로가기",
                    descriptor.getRedirectUrl() != null && !descriptor.getRedirectUrl().isBlank()
                            ? descriptor.getRedirectUrl()
                            : "/home"
            );

            return new AgentTool.ToolResult(summary.toString().trim(), component, componentData);

        } catch (Exception e) {
            log.error("[InProcessApiDispatcher] 도구 {} 실행 실패: {}", descriptor.getName(), e.getMessage(), e);
            return new AgentTool.ToolResult(descriptor.getCardTitle() + " 조회 중 오류가 발생했습니다.", null, null);
        }
    }

    private Object convertValue(Object val, Class<?> targetType) {
        if (val == null) {
            if (targetType.isPrimitive()) {
                if (targetType == boolean.class) return false;
                if (targetType == int.class) return 0;
                if (targetType == long.class) return 0L;
            }
            return null;
        }

        String str = String.valueOf(val).trim();
        if (targetType == String.class) return str;
        if (targetType == int.class || targetType == Integer.class) {
            try { return Integer.parseInt(str); } catch (Exception e) { return 0; }
        }
        if (targetType == long.class || targetType == Long.class) {
            try { return Long.parseLong(str); } catch (Exception e) { return 0L; }
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(str);
        }

        try {
            return objectMapper.convertValue(val, targetType);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatItemSummary(Object item) {
        if (item == null) return "";
        try {
            Map<?, ?> map = objectMapper.convertValue(item, Map.class);
            String title = "";
            if (map.containsKey("name") && map.get("name") != null) title = String.valueOf(map.get("name"));
            else if (map.containsKey("title") && map.get("title") != null) title = String.valueOf(map.get("title"));
            else if (map.containsKey("clubName") && map.get("clubName") != null) title = String.valueOf(map.get("clubName"));

            String sub = "";
            if (map.containsKey("category") && map.get("category") != null) sub = String.valueOf(map.get("category"));
            else if (map.containsKey("content") && map.get("content") != null) sub = String.valueOf(map.get("content"));

            if (!title.isBlank() && !sub.isBlank()) {
                return String.format("[%s] %s", sub, title);
            } else if (!title.isBlank()) {
                return title;
            } else {
                return map.toString();
            }
        } catch (Exception e) {
            return String.valueOf(item);
        }
    }
}
