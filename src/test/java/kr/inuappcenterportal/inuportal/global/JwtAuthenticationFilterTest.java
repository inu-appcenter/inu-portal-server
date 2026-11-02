package kr.inuappcenterportal.inuportal.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.firebase.contorller.FcmController;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.TokenRequestDto;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmRetryService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.LiveActivityLifecycleService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.NotificationViewService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.ScheduledNotificationService;
import kr.inuappcenterportal.inuportal.global.config.CustomResourceResolver;
import kr.inuappcenterportal.inuportal.global.config.SecurityConfig;
import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FcmController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import(SecurityConfig.class)
class JwtAuthenticationFilterTest {

    private static final String EXPIRED = "expired-token";

    @Autowired
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    CustomResourceResolver customResourceResolver;
    @MockBean
    TokenProvider tokenProvider;
    @MockBean
    FcmService fcmService;
    @MockBean
    FcmAsyncService fcmAsyncService;
    @MockBean
    ScheduledNotificationService scheduledNotificationService;
    @MockBean
    FcmRetryService fcmRetryService;
    @MockBean
    NotificationViewService notificationViewService;
    @MockBean
    LiveActivityLifecycleService liveActivityLifecycleService;

    private void givenExpiredToken() {
        given(tokenProvider.resolveToken(any())).willReturn(EXPIRED);
        given(tokenProvider.validateToken(EXPIRED)).willThrow(new MyException(MyErrorCode.EXPIRED_TOKEN));
    }

    @Test
    @DisplayName("만료 토큰이어도 permitAll 엔드포인트(POST /api/tokens)는 익명으로 통과한다")
    void expiredTokenPassesPermitAll() throws Exception {
        givenExpiredToken();
        String body = objectMapper.writeValueAsString(TokenRequestDto.builder().token("fcm-token").build());

        mockMvc.perform(post("/api/tokens").header("Auth", EXPIRED).content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("토큰 등록 성공"));
        verify(fcmService).saveToken(any(TokenRequestDto.class), isNull());
    }

    @Test
    @DisplayName("만료 토큰으로 인증 필요 엔드포인트 호출 시 기존과 같은 만료 응답을 반환한다")
    void expiredTokenOnProtectedEndpointKeepsExpiredResponse() throws Exception {
        givenExpiredToken();

        mockMvc.perform(put("/api/tokens/live-activity").header("Auth", EXPIRED).content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("만료된 토큰입니다."))
                .andExpect(jsonPath("$.data").value(-1));
    }

    @Test
    @DisplayName("토큰 없이 인증 필요 엔드포인트 호출 시 인증 실패 응답을 반환한다")
    void noTokenOnProtectedEndpoint() throws Exception {
        mockMvc.perform(put("/api/tokens/live-activity").content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("인증이 실패하였습니다."));
    }
}
