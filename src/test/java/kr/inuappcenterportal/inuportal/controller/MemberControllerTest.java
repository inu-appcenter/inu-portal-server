package kr.inuappcenterportal.inuportal.controller;

import com.google.gson.Gson;
import kr.inuappcenterportal.inuportal.dto.LoginDto;
import kr.inuappcenterportal.inuportal.dto.TokenDto;
import kr.inuappcenterportal.inuportal.service.MemberService;
import kr.inuappcenterportal.inuportal.service.PostService;
import kr.inuappcenterportal.inuportal.service.ReplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(MemberController.class)
@WithMockUser
@MockBean(JpaMetamodelMappingContext.class)
public class MemberControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    MemberService memberService;
    @MockBean
    PostService postService;
    @MockBean
    ReplyService replyService;



    @Test
    @DisplayName("로그인 테스트")
    void loginTest() throws Exception{
        TokenDto tokenDto = TokenDto.of("123","456","15","18");
        LoginDto loginDto = LoginDto.builder().studentId("201901591").password("12345").build();
        given(memberService.schoolLogin(loginDto)).willReturn(tokenDto);
        Gson gson = new Gson();
        String body = gson.toJson(loginDto);
        mockMvc.perform(post("/api/members/login").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("로그인 성공, 토근이 발급되었습니다."))
                .andDo(print());
        verify(memberService).schoolLogin(any(LoginDto.class));
    }



}
