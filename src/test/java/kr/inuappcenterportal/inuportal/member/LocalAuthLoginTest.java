package kr.inuappcenterportal.inuportal.member;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.member.dto.LoginDto;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.domain.weather.service.WeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles({"test", "local"})
class LocalAuthLoginTest {

    @MockBean
    WeatherService weatherService;
    @MockBean
    ScheduleService scheduleService;
    @MockBean
    NoticeService noticeService;
    @MockBean
    CafeteriaService cafeteriaService;
    @MockBean
    FirebaseMessaging firebaseMessaging;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("local profile seeded admin can log in and call admin api")
    void localAdminLoginAndAdminApiTest() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/members/login")
                        .content(makeLoginBody("local-admin", "local-admin"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andDo(print())
                .andReturn();

        String accessToken = extractAccessToken(loginResult);

        mockMvc.perform(get("/api/members")
                        .header("Auth", accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("admin"))
                .andDo(print());

        mockMvc.perform(get("/api/members/all")
                        .header("Auth", accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    private String makeLoginBody(String studentId, String password) throws JsonProcessingException {
        LoginDto loginDto = LoginDto.builder()
                .studentId(studentId)
                .password(password)
                .build();
        return objectMapper.writeValueAsString(loginDto);
    }

    private String extractAccessToken(MvcResult loginResult) throws Exception {
        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("accessToken").asText();
    }
}
