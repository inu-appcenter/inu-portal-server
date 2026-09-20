package kr.inuappcenterportal.inuportal.domain.agent.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/** 라우팅 모델의 JSON 값을 중첩 객체와 배열까지 손실 없이 Java 값으로 변환한다. */
public final class AgentToolJsonParser {
    private AgentToolJsonParser() {}

    public static Map<String, Object> toMap(ObjectMapper objectMapper, JsonNode paramsNode) {
        if (paramsNode == null || !paramsNode.isObject()) return new LinkedHashMap<>();
        return objectMapper.convertValue(paramsNode, new TypeReference<LinkedHashMap<String, Object>>() {});
    }
}
