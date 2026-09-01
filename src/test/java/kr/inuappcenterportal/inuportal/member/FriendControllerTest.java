package kr.inuappcenterportal.inuportal.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kr.inuappcenterportal.inuportal.domain.member.controller.FriendController;
import kr.inuappcenterportal.inuportal.domain.member.dto.NearbyFriendResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.service.FriendInviteService;
import kr.inuappcenterportal.inuportal.domain.member.service.FriendService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import(SecurityConfig.class)
public class FriendControllerTest {

    @MockBean
    private CustomResourceResolver customResourceResolver;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendService friendService;

    @MockBean
    private FriendInviteService friendInviteService;

    @MockBean
    private TokenProvider tokenProvider;

    @Test
    @DisplayName("주변 친구 후보 조회 컨트롤러 테스트")
    void getNearbyFriendsTest() throws Exception {
        Member authMember = mock(Member.class);
        when(authMember.getId()).thenReturn(1L);
        when(authMember.getUsername()).thenReturn("1");

        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        List<NearbyFriendResponseDto> list = List.of(
                NearbyFriendResponseDto.builder()
                        .memberId(42L)
                        .nickname("202201543")
                        .studentId("2022***43")
                        .fireId(3L)
                        .distanceMeters(87L)
                        .build()
        );

        when(friendService.getNearbyFriends(eq(1L), eq(37.4638), eq(126.6321), eq(200))).thenReturn(list);

        mockMvc.perform(get("/api/friends/nearby")
                        .header("Auth", token)
                        .param("latitude", "37.4638")
                        .param("longitude", "126.6321")
                        .param("radiusMeters", "200")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("주변 친구 후보 조회 성공"))
                .andExpect(jsonPath("$.data[0].memberId").value(42))
                .andExpect(jsonPath("$.data[0].nickname").value("202201543"))
                .andExpect(jsonPath("$.data[0].studentId").value("2022***43"))
                .andExpect(jsonPath("$.data[0].fireId").value(3))
                .andExpect(jsonPath("$.data[0].distanceMeters").value(87))
                .andDo(print());

        verify(friendService).getNearbyFriends(eq(1L), eq(37.4638), eq(126.6321), eq(200));
    }
}
