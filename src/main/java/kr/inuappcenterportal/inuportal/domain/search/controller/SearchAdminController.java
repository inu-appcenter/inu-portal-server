package kr.inuappcenterportal.inuportal.domain.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.search.service.SearchIndexSyncService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "SearchAdmin", description = "검색 엔진 색인 관리자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search/admin")
public class SearchAdminController {

    private final SearchIndexSyncService searchIndexSyncService;

    @Operation(summary = "전체 도메인 데이터 재색인 (초기 백필)", description = "MySQL DB에 존재하는 모든 공지사항, 학과공지, 게시글, 학사일정, 전화번호부, 강의, 동아리 데이터를 Elasticsearch로 재색인합니다.")
    @ApiResponse(responseCode = "200", description = "재색인 성공")
    @PostMapping("/reindex")
    public ResponseEntity<ResponseDto<Integer>> reindexAll() {
        int count = searchIndexSyncService.syncAll();
        return ResponseEntity.ok(ResponseDto.of(count, "전체 " + count + "건 데이터 재색인 완료"));
    }

    @Operation(summary = "특정 도메인 데이터 재색인", description = "도메인 지정: notices, dept-notices, posts, schedules, directory, courses, clubs")
    @ApiResponse(responseCode = "200", description = "특정 도메인 재색인 성공")
    @PostMapping("/reindex/{domain}")
    public ResponseEntity<ResponseDto<Integer>> reindexDomain(
            @Parameter(description = "재색인 대상 도메인 (notices, dept-notices, posts, schedules, directory, courses, clubs)", example = "notices")
            @PathVariable String domain
    ) {
        int count = switch (domain.toLowerCase()) {
            case "notices" -> searchIndexSyncService.syncNotices();
            case "dept-notices", "department-notices" -> searchIndexSyncService.syncDepartmentNotices();
            case "posts" -> searchIndexSyncService.syncPosts();
            case "schedules" -> searchIndexSyncService.syncSchedules();
            case "directory" -> searchIndexSyncService.syncDirectory();
            case "courses" -> searchIndexSyncService.syncCourses();
            case "clubs" -> searchIndexSyncService.syncClubs();
            default -> throw new IllegalArgumentException("지원하지 않는 도메인입니다: " + domain);
        };
        return ResponseEntity.ok(ResponseDto.of(count, domain + " 도메인 " + count + "건 재색인 완료"));
    }
}
