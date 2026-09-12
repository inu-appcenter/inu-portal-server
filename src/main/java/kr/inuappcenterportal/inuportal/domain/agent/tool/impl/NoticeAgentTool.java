package kr.inuappcenterportal.inuportal.domain.agent.tool.impl;

import kr.inuappcenterportal.inuportal.domain.agent.dto.UiComponentDto;
import kr.inuappcenterportal.inuportal.domain.agent.tool.AgentTool;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.notice.dto.NoticeListResponseDto;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import kr.inuappcenterportal.inuportal.domain.notice.repository.DepartmentNoticeRepository;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeAgentTool implements AgentTool {

    private final NoticeService noticeService;
    private final DepartmentNoticeRepository departmentNoticeRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

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

            Department userDept = (member != null) ? member.getDepartment() : null;
            String deptName = (userDept != null) ? userDept.getDepartmentName() : "학과";

            List<NoticeListResponseDto> schoolNotices = Collections.emptyList();
            long schoolTotal = 0;

            List<DepartmentNotice> deptNotices = Collections.emptyList();
            long deptTotal = 0;

            if (query.length() >= 2) {
                ListResponseDto<NoticeListResponseDto> schoolResult = noticeService.searchNotice(query, null, 1);
                if (schoolResult != null && schoolResult.getContents() != null) {
                    schoolNotices = schoolResult.getContents();
                    schoolTotal = schoolResult.getTotal();
                }

                Page<DepartmentNotice> deptPage = departmentNoticeRepository.searchDepartmentNotices(
                        userDept, query, PageRequest.of(0, 4));
                if (deptPage != null) {
                    deptNotices = deptPage.getContent();
                    deptTotal = deptPage.getTotalElements();
                }
            } else {
                schoolNotices = noticeService.getTop();
                schoolTotal = schoolNotices != null ? schoolNotices.size() : 0;

                if (userDept != null) {
                    Page<DepartmentNotice> deptPage = departmentNoticeRepository.findAllByDepartment(
                            userDept, PageRequest.of(0, 4));
                    if (deptPage != null) {
                        deptNotices = deptPage.getContent();
                        deptTotal = deptPage.getTotalElements();
                    }
                }
            }

            // 통합 UI 카드 데이터 구성
            List<Map<String, Object>> combinedList = new ArrayList<>();

            if (schoolNotices != null) {
                for (NoticeListResponseDto sn : schoolNotices) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", sn.getId());
                    item.put("title", sn.getTitle());
                    item.put("category", sn.getCategory() != null ? sn.getCategory() : "학교");
                    item.put("subCategory", sn.getSubCategory());
                    item.put("writer", sn.getWriter());
                    item.put("createDate", sn.getCreateDate());
                    item.put("url", sn.getUrl());
                    item.put("isDepartment", false);
                    combinedList.add(item);
                }
            }

            if (deptNotices != null) {
                for (DepartmentNotice dn : deptNotices) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", dn.getId());
                    item.put("title", dn.getTitle());
                    item.put("category", dn.getDepartment() != null ? dn.getDepartment().getDepartmentName() : deptName);
                    item.put("subCategory", "학과공지");
                    item.put("writer", dn.getDepartment() != null ? dn.getDepartment().getDepartmentName() : "");
                    item.put("createDate", dn.getCreateDate() != null ? dn.getCreateDate().format(DATE_FORMATTER) : "");
                    item.put("url", dn.getUrl());
                    item.put("views", dn.getView());
                    item.put("isDepartment", true);
                    item.put("department", dn.getDepartment() != null ? dn.getDepartment().name() : null);
                    combinedList.add(item);
                }
            }

            UiComponentDto component = UiComponentDto.of("NOTICE_LIST", combinedList, "공지사항 전체보기", "/home/notice");

            StringBuilder sb = new StringBuilder();
            if (query.length() >= 2) {
                sb.append(String.format("'%s' 검색 결과입니다.\n\n", query));
            } else {
                sb.append("최신 학교 및 학과 공지사항 목록입니다.\n\n");
            }

            boolean hasResults = false;
            if (schoolNotices != null && !schoolNotices.isEmpty()) {
                hasResults = true;
                sb.append(String.format("[학교 공지사항] (%d건)\n", schoolTotal));
                for (int i = 0; i < Math.min(schoolNotices.size(), 3); i++) {
                    NoticeListResponseDto n = schoolNotices.get(i);
                    sb.append(String.format("• [%s] %s (%s)\n", n.getCategory(), n.getTitle(), n.getCreateDate()));
                }
            }

            if (deptNotices != null && !deptNotices.isEmpty()) {
                if (hasResults) sb.append("\n");
                hasResults = true;
                sb.append(String.format("[%s 학과공지] (%d건)\n", deptName, deptTotal));
                for (int i = 0; i < Math.min(deptNotices.size(), 3); i++) {
                    DepartmentNotice dn = deptNotices.get(i);
                    String dateStr = dn.getCreateDate() != null ? dn.getCreateDate().format(DATE_FORMATTER) : "";
                    sb.append(String.format("• %s (%s)\n", dn.getTitle(), dateStr));
                }
            }

            if (!hasResults) {
                sb.append("검색된 학교 및 학과 공지사항이 없습니다.");
            }

            return new ToolResult(sb.toString().trim(), component, combinedList);
        } catch (Exception e) {
            log.error("공지사항 도구 실행 오류: {}", e.getMessage(), e);
            return new ToolResult("공지사항을 검색하는 도중 오류가 발생했습니다.", null, null);
        }
    }

    @Override
    public boolean supportsFallback(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase();
        return lower.contains("공지") || lower.contains("장학") || lower.contains("모집");
    }

    @Override
    public Map<String, Object> createFallbackParams(String message, java.util.List<kr.inuappcenterportal.inuportal.domain.agent.dto.ChatMessageDto> history) {
        return Map.of("query", message != null ? message.trim() : "");
    }
}
