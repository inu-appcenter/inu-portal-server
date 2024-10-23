package kr.inuappcenterportal.inuportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import kr.inuappcenterportal.inuportal.dto.LoginDto;
import kr.inuappcenterportal.inuportal.dto.TokenDto;
import kr.inuappcenterportal.inuportal.exception.MyExceptionHandler;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.service.MemberService;
import kr.inuappcenterportal.inuportal.service.PostService;
import kr.inuappcenterportal.inuportal.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebClientBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
@MockBean(JpaMetamodelMappingContext.class)
public class MemberControllerTest {
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private MemberController memberController;
    @Mock
    MemberService memberService;
    @MockBean
    ReplyService replyService;
    @MockBean
    PostService postService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController).setControllerAdvice(new MyExceptionHandler()).build();
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccessTest() throws Exception{
        TokenDto tokenDto = TokenDto.of("123","456","15","18");
        LoginDto loginDto = LoginDto.builder().studentId("201901591").password("12345").build();
        given(memberService.schoolLogin(any(LoginDto.class))).willReturn(tokenDto);
        String body = objectMapper.writeValueAsString(loginDto);
        mockMvc.perform(post("/api/members/login").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("로그인 성공, 토근이 발급되었습니다."))
                .andDo(print());
        verify(memberService).schoolLogin(any(LoginDto.class));
    }



    @Test
    @DisplayName("로그인 실패 테스트")
    void loginFailTest() throws Exception{
        LoginDto loginDto = LoginDto.builder().studentId("201901591").password("12345").build();
        when(memberService.schoolLogin(any(LoginDto.class))).thenThrow(new MyException(MyErrorCode.STUDENT_LOGIN_ERROR));
        String body = objectMapper.writeValueAsString(loginDto);
        mockMvc.perform(post("/api/members/login").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("학번 또는 비밀번호가 틀립니다."))
                .andDo(print());

    }



}
