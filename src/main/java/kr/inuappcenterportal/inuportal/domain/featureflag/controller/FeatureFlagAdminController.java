package kr.inuappcenterportal.inuportal.domain.featureflag.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.featureflag.dto.FeatureFlagCreateRequest;
import kr.inuappcenterportal.inuportal.domain.featureflag.dto.FeatureFlagResponse;
import kr.inuappcenterportal.inuportal.domain.featureflag.dto.FeatureFlagUpdateRequest;
import kr.inuappcenterportal.inuportal.domain.featureflag.service.FeatureFlagService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/feature-flags")
public class FeatureFlagAdminController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    public ResponseEntity<ResponseDto<List<FeatureFlagResponse>>> getAdminFlags() {
        return ResponseEntity.ok(
                ResponseDto.of(featureFlagService.getAdminFlags(), "Feature flag 목록 조회 성공")
        );
    }

    @PostMapping
    public ResponseEntity<ResponseDto<FeatureFlagResponse>> create(
            @Valid @RequestBody FeatureFlagCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseDto.of(featureFlagService.create(request), "Feature flag 생성 성공")
        );
    }

    @PatchMapping("/{key}")
    public ResponseEntity<ResponseDto<FeatureFlagResponse>> update(
            @PathVariable String key,
            @Valid @RequestBody FeatureFlagUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ResponseDto.of(featureFlagService.update(key, request), "Feature flag 수정 성공")
        );
    }
}
