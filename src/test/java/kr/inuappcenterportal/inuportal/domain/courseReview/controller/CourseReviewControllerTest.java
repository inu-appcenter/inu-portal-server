package kr.inuappcenterportal.inuportal.domain.courseReview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kr.inuappcenterportal.inuportal.domain.courseReview.service.CourseReviewService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.config.CustomResourceResolver;
import kr.inuappcenterportal.inuportal.global.config.SecurityConfig;
import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseReviewController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import(SecurityConfig.class)
public class CourseReviewControllerTest {

    @MockBean
    private CustomResourceResolver customResourceResolver;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseReviewService courseReviewService;

    @MockBean
    private TokenProvider tokenProvider;

    @Test
    @DisplayName("강의평 목록 조회 실패 - 비로그인(인증 없음) 시 SecurityConfig에 의해 401 Unauthorized 차단")
    public void getCourseReviews_fail_unauthorized() throws Exception {
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(null);

        mockMvc.perform(get("/api/courses/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("인증이 실패하였습니다."));

        verify(courseReviewService, never()).getReviews(any(), any(), any(), any());
    }

    @Test
    @DisplayName("최근 강의평 피드 조회 실패 - 비로그인(인증 없음) 시 SecurityConfig에 의해 401 Unauthorized 차단")
    public void getRecentReviews_fail_unauthorized() throws Exception {
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(null);

        mockMvc.perform(get("/api/course-reviews/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("인증이 실패하였습니다."));

        verify(courseReviewService, never()).getRecentReviews(anyInt(), any());
    }

    @Test
    @DisplayName("최근 강의평 목록 조회 성공 - 로그인(USER 권한) 사용자")
    public void getRecentReviews_success_authenticated() throws Exception {
        Member authMember = mock(Member.class);
        String token = "userToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(courseReviewService.getRecentReviews(anyInt(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/course-reviews/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("최근 강의평 목록 조회 성공"));

        verify(courseReviewService).getRecentReviews(anyInt(), any());
    }
}
