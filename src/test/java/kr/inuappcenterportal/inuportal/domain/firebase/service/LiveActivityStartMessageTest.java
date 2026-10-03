package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.messaging.Message;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.LiveActivityStartPush;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

class LiveActivityStartMessageTest {

    @Test
    @DisplayName("push-to-start 메시지는 FCM 토큰과 ActivityKit 토큰을 함께 싣고, aps에 ActivityKit 시작 규격을 채운다")
    void buildsPushToStartPayload() throws Exception {
        LiveActivityStartPush push = new LiveActivityStartPush(
                "TimetableLiveActivity", "{\"phase\":\"UPCOMING\"}", 1_800_000_000L, 1_800_000_000L);

        Message message = FcmService.createLiveActivityStartMessage(
                "fcm-token", "la-token", "10분 후 수업이 시작돼요.", "자료구조 (10:30~11:45)", push, 1_799_999_400L);

        // FCM으로 나가는 실제 JSON(SDK와 같은 Gson 직렬화)으로 검증한다.
        JsonNode json = new ObjectMapper().readTree(GsonFactory.getDefaultInstance().toString(message));

        assertThat(json.path("token").asText()).isEqualTo("fcm-token");
        assertThat(json.has("notification")).isFalse();

        JsonNode apns = json.path("apns");
        assertThat(apns.path("live_activity_token").asText()).isEqualTo("la-token");
        assertThat(apns.path("headers").path("apns-priority").asText()).isEqualTo("10");
        assertThat(apns.path("headers").path("apns-expiration").asText()).isEqualTo("1800000000");

        JsonNode aps = apns.path("payload").path("aps");
        assertThat(aps.path("event").asText()).isEqualTo("start");
        assertThat(aps.path("timestamp").asLong()).isEqualTo(1_799_999_400L);
        assertThat(aps.path("attributes-type").asText()).isEqualTo("LiveActivityAttributes");
        assertThat(aps.path("attributes").isObject()).isTrue();
        assertThat(aps.path("attributes").size()).isZero();
        assertThat(aps.path("stale-date").asLong()).isEqualTo(1_800_000_000L);
        assertThat(aps.path("content-state").path("name").asText()).isEqualTo("TimetableLiveActivity");
        assertThat(aps.path("content-state").path("props").asText()).isEqualTo("{\"phase\":\"UPCOMING\"}");
        assertThat(aps.path("alert").path("title").asText()).isEqualTo("10분 후 수업이 시작돼요.");
        assertThat(aps.path("alert").path("body").asText()).isEqualTo("자료구조 (10:30~11:45)");
    }
}
