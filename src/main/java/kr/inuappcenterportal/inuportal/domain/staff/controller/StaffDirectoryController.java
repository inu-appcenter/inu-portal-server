package kr.inuappcenterportal.inuportal.domain.staff.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.staff.dto.StaffDirectoryEntryResponse;
import kr.inuappcenterportal.inuportal.domain.staff.dto.StaffDirectorySyncResponse;
import kr.inuappcenterportal.inuportal.domain.staff.enums.StaffDirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.staff.service.StaffDirectoryService;
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

@Tag(name = "StaffDirectory", description = "Staff directory API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff-directory")
public class StaffDirectoryController {

    private final StaffDirectoryService staffDirectoryService;

    @Operation(summary = "Get staff directory entries", description = "Currently serves headquarters and affiliated institution data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff directory lookup succeeded",
                    content = @Content(schema = @Schema(implementation = StaffDirectoryEntryResponse.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<ListResponseDto<StaffDirectoryEntryResponse>>> getStaffDirectory(
            @RequestParam(required = false) StaffDirectoryCategory category,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page) {
        return ResponseEntity.ok(
                ResponseDto.of(staffDirectoryService.getEntries(category, page), "Staff directory lookup succeeded")
        );
    }

    @Operation(summary = "Run a manual staff directory sync", description = "Recrawls every page for headquarters and affiliated institutions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff directory sync succeeded",
                    content = @Content(schema = @Schema(implementation = StaffDirectorySyncResponse.class)))
    })
    @PostMapping("/sync")
    public ResponseEntity<ResponseDto<StaffDirectorySyncResponse>> syncStaffDirectory() throws IOException {
        return ResponseEntity.ok(
                ResponseDto.of(staffDirectoryService.syncCrawlableCategories(), "Staff directory sync succeeded")
        );
    }
}
