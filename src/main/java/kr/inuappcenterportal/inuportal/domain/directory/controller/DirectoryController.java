package kr.inuappcenterportal.inuportal.domain.directory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.directory.dto.CollegeOfficeContactResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.CollegeOfficeContactSyncResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectoryEntryResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectorySourceResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectorySourceSyncResponse;
import kr.inuappcenterportal.inuportal.domain.directory.dto.DirectorySyncResponse;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.service.CollegeOfficeContactService;
import kr.inuappcenterportal.inuportal.domain.directory.service.DirectoryService;
import kr.inuappcenterportal.inuportal.domain.directory.service.DirectorySourceService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "전화번호부", description = "교내 행정기관, 대학, 대학원 연락처를 조회하는 전화번호부 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directoryService;
    private final DirectorySourceService directorySourceService;
    private final CollegeOfficeContactService collegeOfficeContactService;

    @Operation(summary = "교내 전화번호부 조회", description = "본부, 부속기관, 대학, 대학원 소속 연락처의 전화번호, 이메일, 담당 업무 등 교내 전화번호부 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "교내 전화번호부 조회 성공",
                    content = @Content(schema = @Schema(implementation = DirectoryEntryResponse.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<ListResponseDto<DirectoryEntryResponse>>> getDirectory(
            @RequestParam(required = false) DirectoryCategory category,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(
                ResponseDto.of(directoryService.getEntries(category, query, page), "교내 전화번호부 조회 성공")
        );
    }

    @Operation(summary = "교내 전화번호부 검색", description = "본부, 부속기관, 대학, 대학원 소속 연락처의 전화번호, 이메일, 담당 업무 등 교내 전화번호부 정보를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "교내 전화번호부 검색 성공",
                    content = @Content(schema = @Schema(implementation = DirectoryEntryResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<ListResponseDto<DirectoryEntryResponse>>> searchDirectory(
            @RequestParam(required = false) DirectoryCategory category,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(
                ResponseDto.of(directoryService.getEntries(category, query, page), "교내 전화번호부 검색 성공")
        );
    }

    @Operation(summary = "교내 전화번호부 수동 동기화", description = "전화번호부 수집 대상 사이트 목록을 새로고침하고, 지원하는 모든 교내 전화번호부 항목을 다시 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "교내 전화번호부 동기화 성공",
                    content = @Content(schema = @Schema(implementation = DirectorySyncResponse.class)))
    })
    @PostMapping("/sync")
    public ResponseEntity<ResponseDto<DirectorySyncResponse>> syncDirectory() throws IOException {
        return ResponseEntity.ok(
                ResponseDto.of(directoryService.syncAllCategories(), "교내 전화번호부 동기화 성공")
        );
    }

    @Operation(summary = "전화번호부 수집 대상 사이트 조회", description = "교내 전화번호부를 수집하는 대학 및 대학원 사이트 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전화번호부 수집 대상 사이트 조회 성공",
                    content = @Content(schema = @Schema(implementation = DirectorySourceResponse.class)))
    })
    @GetMapping("/sources")
    public ResponseEntity<ResponseDto<ListResponseDto<DirectorySourceResponse>>> getDirectorySources(
            @RequestParam(required = false) DirectoryCategory category,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(
                ResponseDto.of(directorySourceService.getSources(category, page), "전화번호부 수집 대상 목록 조회 성공")
        );
    }

    @Operation(summary = "전화번호부 수집 대상 사이트 수동 동기화", description = "교내 전화번호부를 수집하는 대학 및 대학원 사이트 목록을 다시 수집합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전화번호부 수집 대상 사이트 동기화 성공",
                    content = @Content(schema = @Schema(implementation = DirectorySourceSyncResponse.class)))
    })
    @PostMapping("/sources/sync")
    public ResponseEntity<ResponseDto<DirectorySourceSyncResponse>> syncDirectorySources() throws IOException {
        return ResponseEntity.ok(
                ResponseDto.of(directorySourceService.syncInventoryCategories(), "전화번호부 수집 대상 목록 동기화 성공")
        );
    }

    @Operation(summary = "단과대학 학과사무실 전화번호부 조회", description = "ISC 단과대학 연락처 페이지에서 학과 사무실의 전화번호, 홈페이지 URL, 위치 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "단과대학 행정실 전화번호부 조회 성공",
                    content = @Content(schema = @Schema(implementation = CollegeOfficeContactResponse.class)))
    })
    @GetMapping("/college-office-contacts")
    public ResponseEntity<ResponseDto<ListResponseDto<CollegeOfficeContactResponse>>> getCollegeOfficeContacts(
            @RequestParam(required = false) String collegeName,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(
                ResponseDto.of(
                        collegeOfficeContactService.getContacts(collegeName, query, page),
                        "학과 행정실 연락처 조회 성공"
                )
        );
    }

    @Operation(summary = "단과대학 행정실 전화번호부 검색", description = "ISC 단과대학 연락처 페이지에서 학과 사무실의 전화번호, 홈페이지 URL, 위치 정보를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "단과대학 행정실 전화번호부 검색 성공",
                    content = @Content(schema = @Schema(implementation = CollegeOfficeContactResponse.class)))
    })
    @GetMapping("/college-office-contacts/search")
    public ResponseEntity<ResponseDto<ListResponseDto<CollegeOfficeContactResponse>>> searchCollegeOfficeContacts(
            @RequestParam(required = false) String collegeName,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(
                ResponseDto.of(
                        collegeOfficeContactService.getContacts(collegeName, query, page),
                        "학과 행정실 연락처 검색 성공"
                )
        );
    }

    @Operation(summary = "단과대학 행정실 전화번호부 수동 동기화", description = "ISC 단과대학 연락처 페이지를 다시 수집해 학과 사무실 전화번호부 데이터를 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "단과대학 행정실 전화번호부 동기화 성공",
                    content = @Content(schema = @Schema(implementation = CollegeOfficeContactSyncResponse.class)))
    })
    @PostMapping("/college-office-contacts/sync")
    public ResponseEntity<ResponseDto<CollegeOfficeContactSyncResponse>> syncCollegeOfficeContacts() throws IOException {
        return ResponseEntity.ok(
                ResponseDto.of(collegeOfficeContactService.sync(), "학과 행정실 연락처 동기화 성공")
        );
    }
}
