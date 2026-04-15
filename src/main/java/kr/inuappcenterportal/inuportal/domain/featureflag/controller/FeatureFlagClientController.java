package kr.inuappcenterportal.inuportal.domain.featureflag.controller;

import kr.inuappcenterportal.inuportal.domain.featureflag.service.FeatureFlagService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feature-flags")
public class FeatureFlagClientController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    public ResponseEntity<ResponseDto<Map<String, Boolean>>> getClientFlags() {
        return ResponseEntity.ok(
                ResponseDto.of(featureFlagService.getClientFlags(), "Feature flag 조회 성공")
        );
    }
}
