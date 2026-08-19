package kr.inuappcenterportal.inuportal.suggestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.suggestion.controller.SuggestionController;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionAnswerRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionListResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.enums.SuggestionCategory;
import kr.inuappcenterportal.inuportal.domain.suggestion.model.Suggestion;
import kr.inuappcenterportal.inuportal.domain.suggestion.service.SuggestionService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuggestionController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import(SecurityConfig.class)
public class SuggestionControllerTest {
    @MockBean
    private CustomResourceResolver customResourceResolver;
    @Autowired
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    SuggestionController suggestionController;
    @MockBean
    SuggestionService suggestionService;
    @MockBean
    TokenProvider tokenProvider;

    @Test
    @DisplayName("건의사항 등록 테스트")
    public void saveSuggestion() throws Exception {
        Member authMember = mock(Member.class);
        when(authMember.getId()).thenReturn(1L);
        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        SuggestionRequest suggestionRequest = SuggestionRequest.builder()
                .content("이미지 업로드가 안 돼요")
                .category("BUG")
                .build();
        when(suggestionService.saveSuggestion(any(SuggestionRequest.class), any(Member.class))).thenReturn(1L);

        String body = objectMapper.writeValueAsString(suggestionRequest);
        mockMvc.perform(post("/api/suggestions").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("건의사항 등록 성공"))
                .andExpect(jsonPath("$.data").value(1L))
                .andDo(print());
        verify(suggestionService).saveSuggestion(any(SuggestionRequest.class), any(Member.class));
    }

    @Test
    @DisplayName("건의사항 목록조회 테스트")
    public void getSuggestionList() throws Exception {
        Member authMember = mock(Member.class);
        when(authMember.getId()).thenReturn(1L);
        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        Member writer = mock(Member.class);
        when(writer.getId()).thenReturn(1L);
        when(writer.getNickname()).thenReturn("nickname");

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);
        ReflectionTestUtils.setField(suggestion, "createDate", LocalDateTime.now());
        ReflectionTestUtils.setField(suggestion, "modifiedDate", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 8);
        Page<Suggestion> mockPage = new PageImpl<>(List.of(suggestion), pageable, 1);
        SuggestionListResponse suggestionListResponse = SuggestionListResponse.of(mockPage);

        when(suggestionService.getSuggestionList(1, authMember)).thenReturn(suggestionListResponse);

        mockMvc.perform(get("/api/suggestions").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("건의사항 목록 가져오기 성공"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andDo(print());
        verify(suggestionService).getSuggestionList(1, authMember);
    }

    @Test
    @DisplayName("건의사항 상세조회 성공 테스트")
    public void getSuggestion_success() throws Exception {
        Member authMember = mock(Member.class);
        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        Member writer = mock(Member.class);
        when(writer.getId()).thenReturn(1L);
        when(writer.getNickname()).thenReturn("nickname");

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);
        ReflectionTestUtils.setField(suggestion, "createDate", LocalDateTime.now());
        ReflectionTestUtils.setField(suggestion, "modifiedDate", LocalDateTime.now());
        SuggestionResponse suggestionResponse = SuggestionResponse.of(suggestion);

        when(suggestionService.getSuggestion(1L, authMember)).thenReturn(suggestionResponse);

        mockMvc.perform(get("/api/suggestions/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("건의사항 상세 가져오기 성공"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.memberNickname").value("nickname"))
                .andDo(print());
        verify(suggestionService).getSuggestion(1L, authMember);
    }

    @Test
    @DisplayName("건의사항 상세조회 실패 테스트 (권한 없음)")
    public void getSuggestion_fail_authorization() throws Exception {
        Member authMember = mock(Member.class);
        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(suggestionService.getSuggestion(1L, authMember)).thenThrow(new MyException(MyErrorCode.HAS_NOT_SUGGESTION_AUTHORIZATION));

        mockMvc.perform(get("/api/suggestions/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value(MyErrorCode.HAS_NOT_SUGGESTION_AUTHORIZATION.getMessage()))
                .andDo(print());
    }

    @Test
    @DisplayName("건의사항 상세조회 실패 테스트 (존재하지 않음)")
    public void getSuggestion_fail_notFound() throws Exception {
        Member authMember = mock(Member.class);
        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(suggestionService.getSuggestion(1L, authMember)).thenThrow(new MyException(MyErrorCode.SUGGESTION_NOT_FOUND));

        mockMvc.perform(get("/api/suggestions/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.msg").value(MyErrorCode.SUGGESTION_NOT_FOUND.getMessage()))
                .andDo(print());
    }

    @Test
    @DisplayName("건의사항 삭제 테스트")
    public void deleteSuggestion() throws Exception {
        Member authMember = mock(Member.class);
        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(suggestionService.deleteSuggestion(1L, authMember)).thenReturn(1L);

        mockMvc.perform(delete("/api/suggestions/1").with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("건의사항 삭제 성공"))
                .andExpect(jsonPath("$.data").value(1L))
                .andDo(print());
        verify(suggestionService).deleteSuggestion(1L, authMember);
    }

    @Test
    @DisplayName("건의사항 답변등록 테스트 (관리자)")
    public void answerSuggestion_admin() throws Exception {
        Member authMember = mock(Member.class);
        String token = "adminToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        SuggestionAnswerRequest suggestionAnswerRequest = SuggestionAnswerRequest.builder()
                .status("COMPLETED")
                .answerContent("다음 업데이트에 반영했습니다.")
                .build();
        when(suggestionService.answerSuggestion(eq(1L), any(SuggestionAnswerRequest.class))).thenReturn(1L);

        String body = objectMapper.writeValueAsString(suggestionAnswerRequest);
        mockMvc.perform(patch("/api/suggestions/1/answer").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("건의사항 답변/상태변경 성공"))
                .andExpect(jsonPath("$.data").value(1L))
                .andDo(print());
        verify(suggestionService).answerSuggestion(eq(1L), any(SuggestionAnswerRequest.class));
    }

    @Test
    @DisplayName("건의사항 답변등록 실패 테스트 (USER 권한 - SecurityConfig 차단)")
    public void answerSuggestion_fail_forbiddenForUser() throws Exception {
        Member authMember = mock(Member.class);
        String token = "userToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        SuggestionAnswerRequest suggestionAnswerRequest = SuggestionAnswerRequest.builder()
                .status("COMPLETED")
                .answerContent("다음 업데이트에 반영했습니다.")
                .build();

        String body = objectMapper.writeValueAsString(suggestionAnswerRequest);
        mockMvc.perform(patch("/api/suggestions/1/answer").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("접근 권한이 없는 사용자입니다."))
                .andDo(print());
        verify(suggestionService, never()).answerSuggestion(any(), any());
    }
}
