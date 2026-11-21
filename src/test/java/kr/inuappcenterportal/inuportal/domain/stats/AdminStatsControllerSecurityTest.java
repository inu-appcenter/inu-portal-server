package kr.inuappcenterportal.inuportal.domain.stats;

import kr.inuappcenterportal.inuportal.domain.stats.controller.AdminStatsController;
import kr.inuappcenterportal.inuportal.domain.stats.dto.AdminStatsResponse;
import kr.inuappcenterportal.inuportal.domain.stats.dto.SourceResult;
import kr.inuappcenterportal.inuportal.domain.stats.service.AdminStatsService;
import kr.inuappcenterportal.inuportal.global.config.CustomResourceResolver;
import kr.inuappcenterportal.inuportal.global.config.SecurityConfig;
import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStatsController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import(SecurityConfig.class)
class AdminStatsControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockBean CustomResourceResolver customResourceResolver;
    @MockBean TokenProvider tokenProvider;
    @MockBean AdminStatsService adminStatsService;

    private void givenToken(String token, String role) {
        given(tokenProvider.resolveToken(any())).willReturn(token);
        given(tokenProvider.validateToken(token)).willReturn(true);
        given(tokenProvider.getAuthentication(token)).willReturn(
                new UsernamePasswordAuthenticationToken("user", "", List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    @DisplayName("비로그인은 401")
    void anonymous() throws Exception {
        mockMvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("일반 회원은 403")
    void user() throws Exception {
        givenToken("user-token", "ROLE_USER");
        mockMvc.perform(get("/api/admin/stats").header("Auth", "user-token")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 200, 출처별 status가 응답에 담긴다")
    void admin() throws Exception {
        givenToken("admin-token", "ROLE_ADMIN");
        LocalDate d = LocalDate.of(2026, 11, 11);
        given(adminStatsService.getStats(any(), any(), any())).willReturn(new AdminStatsResponse(
                d, d, OffsetDateTime.now(), SourceResult.failed("서버 통계를 불러오지 못했습니다."),
                SourceResult.notConfigured("Mixpanel 연동 전입니다.")));

        mockMvc.perform(get("/api/admin/stats").header("Auth", "admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.server.status").value("FAILED"))
                .andExpect(jsonPath("$.data.mixpanel.status").value("NOT_CONFIGURED"));
    }

    @Test
    @DisplayName("날짜 형식이 틀리면 400")
    void badDate() throws Exception {
        givenToken("admin-token", "ROLE_ADMIN");
        mockMvc.perform(get("/api/admin/stats").param("from", "2026-13-01").header("Auth", "admin-token"))
                .andExpect(status().isBadRequest());
    }
}
