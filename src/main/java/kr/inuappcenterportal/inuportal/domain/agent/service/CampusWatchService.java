package kr.inuappcenterportal.inuportal.domain.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.agent.dto.CampusWatchCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.CampusWatchJobDto;
import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchDomain;
import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchStatus;
import kr.inuappcenterportal.inuportal.domain.agent.model.CampusWatchJob;
import kr.inuappcenterportal.inuportal.domain.agent.repository.CampusWatchJobRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampusWatchService {

    private final CampusWatchJobRepository watchJobRepository;
    private final FcmService fcmService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private static final String SEAT_ROOMS_URL = "https://lib.inu.ac.kr/pyxis-api/1/seat-rooms?branchGroupId=1&smufMethodCode=PC";

    @Transactional
    public CampusWatchJobDto registerWatch(Member member, CampusWatchCreateRequestDto req) {
        if (member == null) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        int duration = (req.durationMinutes() != null && req.durationMinutes() > 0)
                ? Math.min(180, req.durationMinutes())
                : 90; // 기본 90분

        // 기존 동일 타깃 활성 감시가 있다면 기존 것 반환 또는 갱신
        Optional<CampusWatchJob> existing = watchJobRepository.findByMemberIdAndDomainAndTargetNameAndStatus(
                member.getId(), req.domain(), req.targetName(), CampusWatchStatus.ACTIVE
        );
        if (existing.isPresent()) {
            return CampusWatchJobDto.from(existing.get());
        }

        CampusWatchJob job = CampusWatchJob.builder()
                .member(member)
                .domain(req.domain())
                .targetId(req.targetId() != null ? req.targetId() : req.targetName())
                .targetName(req.targetName())
                .conditionType("AVAILABLE_GT_ZERO")
                .expiresAt(LocalDateTime.now().plusMinutes(duration))
                .build();

        watchJobRepository.save(job);
        log.info("[CampusWatchService] 감시 등록: memberId={}, domain={}, target={}, duration={}분",
                member.getId(), req.domain(), req.targetName(), duration);

        return CampusWatchJobDto.from(job);
    }

    @Transactional(readOnly = true)
    public List<CampusWatchJobDto> getMyWatchJobs(Member member) {
        if (member == null) {
            return Collections.emptyList();
        }
        return watchJobRepository.findAllByMemberIdOrderByCreatedAtDesc(member.getId())
                .stream()
                .map(CampusWatchJobDto::from)
                .toList();
    }

    @Transactional
    public void cancelWatchJob(Member member, Long jobId) {
        if (member == null) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }
        CampusWatchJob job = watchJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("해당 감시 작업을 찾을 수 없습니다."));

        if (!job.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인이 등록한 감시 작업만 취소할 수 있습니다.");
        }

        job.cancel();
        log.info("[CampusWatchService] 감시 취소: jobId={}, memberId={}", jobId, member.getId());
    }

    /**
     * 서버사이드 힐링존/열람실 빈자리 감시 스케줄러 (45초 주기 실행)
     * - Single-Flight: 활성화된 좌석 감시 작업이 있을 때만 외부 API 1회 호출
     */
    @Scheduled(fixedDelay = 45_000, initialDelay = 15_000)
    @Transactional
    public void checkLibrarySeatsWatchJobs() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 만료된 감시 작업 일괄 정리
        watchJobRepository.expireOldJobs(now);

        // 2. 현재 활성화된 도서관 좌석 감시 목록 확인
        List<CampusWatchJob> activeJobs = watchJobRepository.findAllByDomainAndStatus(
                CampusWatchDomain.LIBRARY_SEAT, CampusWatchStatus.ACTIVE
        );

        if (activeJobs.isEmpty()) {
            return; // 구독자가 0명이면 도서관 서버에 요청 일체 보내지 않음 (부하 0)
        }

        // 3. 도서관 공개 API 단 1회 호출 (모든 구독자가 이 1회의 결과를 공유)
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEAT_ROOMS_URL))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) INTIP-Agent/1.0")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null) {
                return;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode listNode = root.path("data").path("list");
            if (!listNode.isArray() || listNode.isEmpty()) {
                return;
            }

            // 룸 이름별 가용 좌석 맵 구축
            Map<String, Integer> availableMap = new HashMap<>();
            for (JsonNode rNode : listNode) {
                String name = rNode.path("name").asText("");
                int available = rNode.path("seats").path("available").asInt(0);
                availableMap.put(name.trim(), available);
            }

            // 4. 감시 중인 작업과 매칭
            for (CampusWatchJob job : activeJobs) {
                String targetName = job.getTargetName();
                // 힐링존, 제1열람실 등 포함 관계 매칭
                for (Map.Entry<String, Integer> entry : availableMap.entrySet()) {
                    if (entry.getKey().contains(targetName) || targetName.contains(entry.getKey())) {
                        int avail = entry.getValue();
                        if (avail > 0) {
                            // 빈자리 발생! 즉시 알림 발송 및 작업 완료
                            sendWatchNotification(job, entry.getKey(), avail);
                            job.markAsNotified();
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("[CampusWatchService] 도서관 좌석 감시 조회 실패: {}", e.getMessage());
        }
    }

    private void sendWatchNotification(CampusWatchJob job, String roomName, int availableCount) {
        String title = String.format("🎉 [%s 빈자리 발생!]", roomName);
        String body = String.format("지금 %s에 빈자리 %d석이 생겼습니다! 서둘러 배정하세요.", roomName, availableCount);
        String route = "https://lib.inu.ac.kr";

        try {
            fcmService.sendDailyBriefNotification(
                    job.getMember().getId(),
                    title,
                    body,
                    FcmMessageType.CAMPUS_WATCH,
                    route
            );
            log.info("[CampusWatchService] 빈자리 푸시 발송 성공: jobId={}, memberId={}, room={}, available={}",
                    job.getId(), job.getMember().getId(), roomName, availableCount);
        } catch (Exception e) {
            log.error("[CampusWatchService] 푸시 발송 실패: jobId={}, err={}", job.getId(), e.getMessage());
        }
    }
}
