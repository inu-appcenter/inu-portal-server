package kr.inuappcenterportal.inuportal.member;

import kr.inuappcenterportal.inuportal.domain.member.controller.MemberController;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.enums.MemberType;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.post.service.PostService;
import kr.inuappcenterportal.inuportal.domain.reply.service.ReplyService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import(SecurityConfig.class)
class FreshmanSecurityWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CustomResourceResolver customResourceResolver;
    @MockBean
    TokenProvider tokenProvider;
    @MockBean
    MemberService memberService;
    @MockBean
    PostService postService;
    @MockBean
    ReplyService replyService;

    @Test
    @DisplayName("비인증 요청은 온보딩·병합·동기화 POST 엔드포인트에서 401로 차단된다")
    void protectedMemberPostEndpointsRequireAuth() throws Exception {
        mockMvc.perform(post("/api/members/onboarding").content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/members/check-merge-freshman").content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/members/sync-academic").content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 신입생 계정의 JWT로 보호된 엔드포인트 호출 시 403 FRESHMAN_EXPIRED 응답을 반환한다")
    void expiredFreshmanJwtReturnsForbidden() throws Exception {
        Member expiredFreshman = Member.builder().studentId("F_202612345").roles(List.of("ROLE_USER")).build();
        expiredFreshman.setMemberType(MemberType.FRESHMAN);
        expiredFreshman.setFreshmanExpiresAt(LocalDateTime.now().minusDays(1));

        given(tokenProvider.resolveToken(any())).willReturn("freshman-jwt");
        given(tokenProvider.validateToken("freshman-jwt")).willReturn(true);
        given(tokenProvider.getAuthentication("freshman-jwt"))
                .willReturn(new UsernamePasswordAuthenticationToken(expiredFreshman, "", expiredFreshman.getAuthorities()));

        mockMvc.perform(post("/api/members/onboarding")
                        .header("Auth", "freshman-jwt")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data").value(-1));
    }

    @Test
    @DisplayName("신입생 로그인 POST는 비인증 상태에서 접근 가능하며 X-Firebase-AppCheck 헤더를 서비스로 전달한다")
    void freshmanLoginIsPermittedWithoutJwt() throws Exception {
        given(memberService.freshmanLogin(any(), any(), any()))
                .willReturn(TokenDto.of("acc", "ref", "2026-12-01T00:00:00", "2027-05-01T00:00:00"));

        mockMvc.perform(post("/api/members/freshman-login")
                        .header("X-Firebase-AppCheck", "appcheck-token")
                        .content("""
                                {"examNo":"202612345","name":"김신입","birthDate":"20070101","deptNm":"컴퓨터공학부"}
                                """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("acc"));
    }
}
