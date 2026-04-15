package kr.inuappcenterportal.inuportal.domain.featureflag.service;

import kr.inuappcenterportal.inuportal.domain.featureflag.dto.FeatureFlagCreateRequest;
import kr.inuappcenterportal.inuportal.domain.featureflag.dto.FeatureFlagResponse;
import kr.inuappcenterportal.inuportal.domain.featureflag.dto.FeatureFlagUpdateRequest;
import kr.inuappcenterportal.inuportal.domain.featureflag.model.FeatureFlag;
import kr.inuappcenterportal.inuportal.domain.featureflag.repository.FeatureFlagRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeatureFlagService {

    private final FeatureFlagRepository repository;

    public Map<String, Boolean> getClientFlags() {
        return repository.findAllByClientVisibleTrue().stream()
                .collect(Collectors.toMap(
                        FeatureFlag::getFlagKey,
                        FeatureFlag::isEnabled,
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
    }

    public List<FeatureFlagResponse> getAdminFlags() {
        return repository.findAll().stream()
                .map(FeatureFlagResponse::from)
                .toList();
    }

    @Transactional
    public FeatureFlagResponse create(FeatureFlagCreateRequest request) {
        String key = request.getKey().trim();

        if (repository.existsByFlagKey(key)) {
            throw new MyException(MyErrorCode.DUPLICATE_FEATURE_FLAG_KEY);
        }

        FeatureFlag flag = repository.save(
                FeatureFlag.builder()
                        .flagKey(key)
                        .enabled(request.getEnabled())
                        .clientVisible(request.getClientVisible())
                        .description(request.getDescription())
                        .build()
        );

        return FeatureFlagResponse.from(flag);
    }

    @Transactional
    public FeatureFlagResponse update(String key, FeatureFlagUpdateRequest request) {
        FeatureFlag flag = repository.findByFlagKey(key)
                .orElseThrow(() -> new MyException(MyErrorCode.FEATURE_FLAG_NOT_FOUND));

        flag.update(
                request.getEnabled(),
                request.getClientVisible(),
                request.getDescription()
        );

        return FeatureFlagResponse.from(flag);
    }

    public boolean isEnabled(String key) {
        return repository.findByFlagKey(key)
                .map(FeatureFlag::isEnabled)
                .orElse(false);
    }

    public void requireEnabled(String key) {
        if (!isEnabled(key)) {
            throw new MyException(MyErrorCode.FEATURE_DISABLED);
        }
    }
}
