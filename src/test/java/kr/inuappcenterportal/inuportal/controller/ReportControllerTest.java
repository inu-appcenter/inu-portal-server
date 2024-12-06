package kr.inuappcenterportal.inuportal.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kr.inuappcenterportal.inuportal.config.SecurityConfig;
import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Report;
import kr.inuappcenterportal.inuportal.dto.ReportListResponseDto;
import kr.inuappcenterportal.inuportal.dto.ReportRequestDto;
import kr.inuappcenterportal.inuportal.service.ReportService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ReportController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import(SecurityConfig.class)
public class ReportControllerTest {
    @Autowired
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    ReportController reportController;
    @MockBean
    ReportService reportService;
    @MockBean
    TokenProvider tokenProvider;


    @Test
    @DisplayName("신고 등록 테스트")
    public void report() throws Exception {
        Member authMember = mock(Member.class);
        when(authMember.getId()).thenReturn(1L);
        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));


        ReportRequestDto dto = new ReportRequestDto("잘못된 정보","7호관은 도서관이 아닙니다.");
        when(reportService.saveReport(any(ReportRequestDto.class),any(Long.class),any(Long.class))).thenReturn(1L);
        String body = objectMapper.writeValueAsString(dto);
        mockMvc.perform(post("/api/reports/1").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("신고하기 성공"))
                .andExpect(jsonPath("$.data").value(1L))
                .andDo(print());
        verify(reportService).saveReport(any(ReportRequestDto.class),any(Long.class),any(Long.class));
    }

    @Test
    @DisplayName("신고목록 가져오기")
    public void getReportList() throws Exception {
        Member authMember = mock(Member.class);
        when(authMember.getId()).thenReturn(1L);
        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        int page = 1;
        Pageable pageable = PageRequest.of(0, 8);
        Report report1 = Report.builder()
                .reason("잘못된 정보")
                .comment("7호관은 도서관이 아닙니다.")
                .postId(101L)
                .memberId(1L)
                .build();

        Report report2 = Report.builder()
                .reason("잘못된 정보")
                .comment("8호관은 도서관이 아닙니다.")
                .postId(102L)
                .memberId(2L)
                .build();

        Report report3 = Report.builder()
                .reason("잘못된 정보")
                .comment("11호관은 도서관이 아닙니다.")
                .postId(105L)
                .memberId(3L)
                .build();

        Class<?> c = report1.getClass();
        Field createDate = c.getSuperclass().getDeclaredField("createDate");
        createDate.setAccessible(true);
        createDate.set(report1,LocalDate.now());
        createDate.set(report2,LocalDate.now());
        createDate.set(report3,LocalDate.now());

        List<Report> reportList = Arrays.asList(report1, report2,report3);
        Page<Report> mockPage = new PageImpl<>(reportList, pageable, reportList.size());
        when(reportService.getReportList(page)).thenReturn(ReportListResponseDto.of(mockPage));

        mockMvc.perform(get("/api/reports").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("신고목록 가져오기 성공"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.pages").value(1))
                .andDo(print());
        verify(reportService).getReportList(1);

    }
}
