package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiveActivityEventMessageTest {

    private static JsonNode json(Message message) throws Exception {
        return new ObjectMapper().readTree(GsonFactory.getDefaultInstance().toString(message));
    }

    @Test
    @DisplayName("update는 Activity 업데이트 토큰으로 content-state와 stale-date를 싣고 alert는 없다")
    void buildsUpdate() throws Exception {
        JsonNode json = json(FcmService.createLiveActivityEventMessage(
                "fcm-token", "activity-token", "update", "TimetableLiveActivity", "{\"phase\":\"ONGOING\"}",
                1_800_004_500L, null, 1_800_000_000L));

        JsonNode apns = json.path("apns");
        assertThat(json.path("token").asText()).isEqualTo("fcm-token");
        assertThat(apns.path("live_activity_token").asText()).isEqualTo("activity-token");
        assertThat(apns.path("headers").path("apns-priority").asText()).isEqualTo("10");

        JsonNode aps = apns.path("payload").path("aps");
        assertThat(aps.path("event").asText()).isEqualTo("update");
        assertThat(aps.path("timestamp").asLong()).isEqualTo(1_800_000_000L);
        assertThat(aps.path("content-state").path("name").asText()).isEqualTo("TimetableLiveActivity");
        assertThat(aps.path("content-state").path("props").asText()).isEqualTo("{\"phase\":\"ONGOING\"}");
        assertThat(aps.path("stale-date").asLong()).isEqualTo(1_800_004_500L);
        assertThat(aps.has("dismissal-date")).isFalse();
        assertThat(aps.has("alert")).isFalse();
        assertThat(aps.has("attributes-type")).isFalse();
    }

    @Test
    @DisplayName("end는 dismissal-date를 싣는다")
    void buildsEnd() throws Exception {
        JsonNode aps = json(FcmService.createLiveActivityEventMessage(
                "fcm-token", "activity-token", "end", "TimetableLiveActivity", "{}",
                null, 1_800_004_560L, 1_800_004_560L)).path("apns").path("payload").path("aps");

        assertThat(aps.path("event").asText()).isEqualTo("end");
        assertThat(aps.path("dismissal-date").asLong()).isEqualTo(1_800_004_560L);
        assertThat(aps.has("stale-date")).isFalse();
    }
}
