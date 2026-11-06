package kr.inuappcenterportal.inuportal.domain.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class InuChatAiServicePseudonymTest {

    private InuChatAiService serviceWithKey(String key) {
        InuChatAiService service = new InuChatAiService(WebClient.builder().build());
        ReflectionTestUtils.setField(service, "pseudonymKey", key);
        return service;
    }

    @Test
    @DisplayName("INUChat 기기 ID에 회원 ID가 그대로 드러나지 않는다")
    void deviceIdDoesNotExposeMemberId() {
        String deviceId = serviceWithKey("test-key").toPseudonymousDeviceId(1234L);

        assertThat(deviceId).startsWith("intip-").isNotEqualTo("intip-1234");
        assertThat(deviceId).doesNotContain("1234");
        assertThat(deviceId).hasSize("intip-".length() + 32);
    }

    @Test
    @DisplayName("같은 회원은 같은 기기 ID를, 다른 회원은 다른 기기 ID를 받는다")
    void deviceIdIsStablePerMember() {
        InuChatAiService service = serviceWithKey("test-key");

        assertThat(service.toPseudonymousDeviceId(1L)).isEqualTo(service.toPseudonymousDeviceId(1L));
        assertThat(service.toPseudonymousDeviceId(1L)).isNotEqualTo(service.toPseudonymousDeviceId(2L));
    }

    @Test
    @DisplayName("키가 다르면 같은 회원이라도 기기 ID가 달라 키 없이는 되돌릴 수 없다")
    void deviceIdDependsOnKey() {
        assertThat(serviceWithKey("key-a").toPseudonymousDeviceId(1L))
                .isNotEqualTo(serviceWithKey("key-b").toPseudonymousDeviceId(1L));
    }

    @Test
    @DisplayName("비회원은 매번 일회용 기기 ID를 받는다")
    void guestGetsOneTimeDeviceId() {
        InuChatAiService service = serviceWithKey("test-key");

        assertThat(service.toPseudonymousDeviceId(null)).isNotEqualTo(service.toPseudonymousDeviceId(null));
    }
}
