package kr.inuappcenterportal.inuportal.domain.agent.tool;

import kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.impl.DirectoryAgentTool;
import kr.inuappcenterportal.inuportal.domain.directory.dto.CollegeOfficeContactResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectoryEntryResponse;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.service.CollegeOfficeContactService;
import kr.inuappcenterportal.inuportal.domain.directory.service.DirectoryService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DirectoryAgentToolTest {

    @Mock
    private CollegeOfficeContactService collegeOfficeContactService;

    @Mock
    private DirectoryService directoryService;

    @InjectMocks
    private DirectoryAgentTool directoryAgentTool;

    @Test
    @DisplayName("교수 성함으로 조회 시 DirectoryService가 호출되고 전화번호와 이메일이 요약 및 컴포넌트에 포함된다")
    void executeWithProfessorNameReturnsDirectoryEntry() {
        DirectoryEntryResponse entry = DirectoryEntryResponse.builder()
                .id(1L)
                .name("박문주")
                .category(DirectoryCategory.UNIVERSITY)
                .categoryName("대학")
                .affiliation("정보기술대학")
                .detailAffiliation("컴퓨터공학부")
                .position("교수")
                .phoneNumber("032-835-8452")
                .email("mjpark@inu.ac.kr")
                .build();

        given(directoryService.getEntries(isNull(), anyString(), anyInt()))
                .willReturn(ListResponseDto.of(1, 1L, List.of(entry)));

        AgentTool.ToolResult result = directoryAgentTool.execute(null, Map.of("query", "박문주"));

        assertNotNull(result.uiComponent());
        assertEquals("DIRECTORY", result.uiComponent().type());
        assertTrue(result.summary().contains("박문주 (교수)"));
        assertTrue(result.summary().contains("032-835-8452"));
        assertTrue(result.summary().contains("mjpark@inu.ac.kr"));
        verify(directoryService).getEntries(null, "박문주", 1);
    }

    @Test
    @DisplayName("호칭이 붙은 '박문주 교수님' 쿼리 시 정제된 '박문주'로 DirectoryService를 재검색한다")
    void executeWithTitleSuffixCleansQueryAndFindsEntry() {
        DirectoryEntryResponse entry = DirectoryEntryResponse.builder()
                .id(1L)
                .name("박문주")
                .position("교수")
                .affiliation("정보기술대학")
                .detailAffiliation("컴퓨터공학부")
                .phoneNumber("032-835-8452")
                .email("mjpark@inu.ac.kr")
                .build();

        // 1차 원본 검색은 실패, 2차 정제 검색은 성공
        given(directoryService.getEntries(isNull(), org.mockito.ArgumentMatchers.eq("박문주 교수님"), anyInt()))
                .willReturn(ListResponseDto.of(0, 0L, Collections.emptyList()));
        given(directoryService.getEntries(isNull(), org.mockito.ArgumentMatchers.eq("박문주"), anyInt()))
                .willReturn(ListResponseDto.of(1, 1L, List.of(entry)));

        AgentTool.ToolResult result = directoryAgentTool.execute(null, Map.of("query", "박문주 교수님"));

        assertNotNull(result.uiComponent());
        assertTrue(result.summary().contains("박문주 (교수)"));
        assertTrue(result.summary().contains("032-835-8452"));
        verify(directoryService).getEntries(null, "박문주", 1);
    }

    @Test
    @DisplayName("query가 일반명사이고 _clientContext에 지도교수명이 있으면 지도교수 성함으로 보정되어 조회된다")
    void executeWithGenericQueryResolvesAdvisorFromClientContext() {
        DirectoryEntryResponse entry = DirectoryEntryResponse.builder()
                .id(1L)
                .name("박문주")
                .position("교수")
                .affiliation("정보기술대학")
                .phoneNumber("032-835-8452")
                .build();

        given(directoryService.getEntries(isNull(), org.mockito.ArgumentMatchers.eq("박문주"), anyInt()))
                .willReturn(ListResponseDto.of(1, 1L, List.of(entry)));

        Map<String, Object> clientContext = Map.of(
                "academicDisplay", Map.of("advisorProfessorName", "박문주")
        );

        AgentTool.ToolResult result = directoryAgentTool.execute(null, Map.of(
                "query", "전화번호나 이메일",
                "_clientContext", clientContext
        ));

        assertNotNull(result.uiComponent());
        assertTrue(result.summary().contains("박문주"));
        verify(directoryService).getEntries(null, "박문주", 1);
    }

    @Test
    @DisplayName("DirectoryService 결과가 없을 경우 학과 사무실(CollegeOfficeContactService)을 대체 검색한다")
    void executeFallsBackToCollegeOfficeContacts() {
        CollegeOfficeContactResponse contact = CollegeOfficeContactResponse.builder()
                .id(10L)
                .departmentName("컴퓨터공학부")
                .collegeName("정보기술대학")
                .officePhoneNumber("032-835-8410")
                .build();

        given(directoryService.getEntries(isNull(), anyString(), anyInt()))
                .willReturn(ListResponseDto.of(0, 0L, Collections.emptyList()));
        given(collegeOfficeContactService.getContacts(isNull(), anyString(), anyInt()))
                .willReturn(ListResponseDto.of(1, 1L, List.of(contact)));

        AgentTool.ToolResult result = directoryAgentTool.execute(null, Map.of("query", "컴퓨터공학부"));

        assertNotNull(result.uiComponent());
        assertTrue(result.summary().contains("컴퓨터공학부"));
        assertTrue(result.summary().contains("032-835-8410"));
        verify(collegeOfficeContactService).getContacts(null, "컴퓨터공학부", 1);
    }

    @Test
    @DisplayName("'컴공 과사' 질의 시 학과 약칭 매핑과 함께 학과 사무실(과사) 번호가 최우선으로 요약에 포함된다")
    void executeWithDepartmentOfficeQueryPrioritizesCollegeOfficeContact() {
        CollegeOfficeContactResponse office = CollegeOfficeContactResponse.builder()
                .id(10L)
                .departmentName("컴퓨터공학부")
                .collegeName("정보기술대학")
                .officePhoneNumber("032-835-8410")
                .officeLocation("7호관 330호")
                .homepageUrl("https://cse.inu.ac.kr")
                .build();

        DirectoryEntryResponse prof = DirectoryEntryResponse.builder()
                .id(1L)
                .name("박문주")
                .position("교수")
                .affiliation("정보기술대학")
                .detailAffiliation("컴퓨터공학부")
                .phoneNumber("032-835-8452")
                .build();

        given(collegeOfficeContactService.getContacts(isNull(), anyString(), anyInt()))
                .willAnswer(invocation -> {
                    String query = invocation.getArgument(1);
                    if ("컴퓨터공학부".equals(query) || "컴공".equals(query)) {
                        return ListResponseDto.of(1, 1L, List.of(office));
                    }
                    return ListResponseDto.of(0, 0L, Collections.emptyList());
                });
        given(directoryService.getEntries(isNull(), anyString(), anyInt()))
                .willAnswer(invocation -> {
                    String query = invocation.getArgument(1);
                    if ("컴퓨터공학부".equals(query) || "컴공".equals(query)) {
                        return ListResponseDto.of(1, 1L, List.of(prof));
                    }
                    return ListResponseDto.of(0, 0L, Collections.emptyList());
                });

        AgentTool.ToolResult result = directoryAgentTool.execute(null, Map.of("query", "컴공 과사 전화번호"));

        assertNotNull(result.uiComponent());
        assertTrue(result.summary().contains("🏢 [학과 사무실(과사)]"));
        assertTrue(result.summary().contains("컴퓨터공학부"));
        assertTrue(result.summary().contains("032-835-8410"));
        assertTrue(result.summary().contains("7호관 330호"));
        assertTrue(result.summary().contains("👨‍🏫 [소속 교수 및 교직원 (참고)]"));
    }

    @Test
    @DisplayName("fallback 시 history에서 직전 발화의 교수 성함을 추출하여 query로 생성한다")
    void fallbackExtractsProfessorNameFromHistory() {
        List<ChatMessageDto> history = List.of(
                new ChatMessageDto("user", "내 지도교수님?"),
                new ChatMessageDto("assistant", "학우님의 지도교수님은 **박문주 교수님**입니다.")
        );

        assertTrue(directoryAgentTool.supportsFallback("전화번호나 이메일 알아?", history));
        Map<String, Object> params = directoryAgentTool.createFallbackParams("전화번호나 이메일 알아?", history);
        assertEquals("박문주", params.get("query"));
    }
}
