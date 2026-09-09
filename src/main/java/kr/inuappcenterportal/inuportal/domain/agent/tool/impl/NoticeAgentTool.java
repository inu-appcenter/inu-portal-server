package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.notice.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeAgentTool implements AgentTool {

    private final NoticeService noticeService;

    @Override
    public String getName() {
        return "NOTICE";
    }

    @Override
    public String getDescription() {
        return "장학금, 대회, 행사, 학과공지, 학교 공지사항 검색 질문 (params: {\"query\": \"검색어(2글자 이상)\"})";
    }

    @Override
    public ToolResult execute(Member member, Map<String, Object> params) {
        try {
            String query = "";
            if (params != null && params.containsKey("query") && params.get("query") != null) {
                query = String.valueOf(params.get("query")).trim();
            }

            ListResponseDto<NoticeListResponseDto> noticeResult;
            if (query.length() >= 2) {
                noticeResult = noticeService.searchNotice(query, null, 1);
            } else {
                List<NoticeListResponseDto> topNotices = noticeService.getTop();
                noticeResult = ListResponseDto.of(topNotices.size(), 1, topNotices);
            }

            List<NoticeListResponseDto> notices = noticeResult.getContents() != null
                    ? noticeResult.getContents()
                    : Collections.emptyList();

            UiComponentDto component = UiComponentDto.of("NOTICE_LIST", notices, "공지사항 전체보기", "/home/notice");

            StringBuilder sb = new StringBuilder();
            if (query.length() >= 2) {
                sb.append(String.format("'%s' 검색 결과 공지사항 %d건을 찾았습니다:\n", query, noticeResult.getTotal()));
            } else {
                sb.append("최신 학교 공지사항 목록입니다:\n");
            }
            if (notices.isEmpty()) {
                sb.append("검색된 공지사항이 없습니다.");
            } else {
                for (int i = 0; i < Math.min(notices.size(), 3); i++) {
                    NoticeListResponseDto n = notices.get(i);
                    sb.append(String.format("• [%s] %s (%s)\n", n.getCategory(), n.getTitle(), n.getCreateDate()));
                }
            }

            return new ToolResult(sb.toString().trim(), component, notices);
        } catch (Exception e) {
            log.error("공지사항 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("공지사항을 검색하는 도중 오류가 발생했습니다.", null, null);
        }
    }
}
