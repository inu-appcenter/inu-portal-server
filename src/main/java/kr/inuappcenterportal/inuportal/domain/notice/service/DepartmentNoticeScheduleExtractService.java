package kr.inuappcenterportal.inuportal.domain.notice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.notice.dto.DepartmentNoticeScheduleExtractItem;
import kr.inuappcenterportal.inuportal.domain.notice.dto.DepartmentNoticeScheduleExtractResponse;
import kr.inuappcenterportal.inuportal.domain.notice.enums.DepartmentNoticeContentStatus;
import kr.inuappcenterportal.inuportal.domain.notice.enums.DepartmentNoticeScheduleExtractStatus;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import kr.inuappcenterportal.inuportal.domain.notice.repository.DepartmentNoticeRepository;
import kr.inuappcenterportal.inuportal.domain.featureflag.service.FeatureFlagService;
import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentNoticeScheduleExtractService {

    private static final String EXTRACT_PATH = "/shared/extract-schedule";
    private static final int EXTRACT_LIMIT_PER_RUN = 20;
    private static final int ERROR_MESSAGE_LIMIT = 500;
    private static final ZoneId EXTRACT_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalTime EXTRACT_WINDOW_START = LocalTime.of(3, 0);
    private static final LocalTime EXTRACT_WINDOW_END = LocalTime.of(7, 0);
    private static final long BATCH_DELAY_MILLIS = 15_000L;

    private final DepartmentNoticeRepository departmentNoticeRepository;
    private final ScheduleRepository scheduleRepository;
    private final DepartmentNoticeScheduleExtractPersistenceService persistenceService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final FeatureFlagService featureFlagService;

    @Value("${app.department-notice.schedule-ai.base-url:}")
    private String baseUrl;

    @Value("${app.department-notice.schedule-ai.api-key:}")
    private String apiKey;

    @Value("${app.department-notice.schedule-ai.timeout-seconds:300}")
    private long timeoutSeconds;

    @Scheduled(cron = "0 * 3-6 * * *")
    public void extractDepartmentNoticeSchedules() {
        if (!featureFlagService.isEnabled("AI_SCHEDULE_EXTRACT_ENABLED")) {
            return;
        }

        if (!isConfigured()) {
            log.warn("학과 공지 AI 일정 추출 설정이 없어 작업을 건너뜁니다. missingBaseUrl={}, missingApiKey={}",
                    isBlank(baseUrl), isBlank(apiKey));
            return;
        }

        int processedCount = 0;
        int successCount = 0;
        int noScheduleCount = 0;
        int failedCount = 0;
        boolean started = false;

        while (isWithinExtractWindow()) {
            List<DepartmentNotice> notices = departmentNoticeRepository.findScheduleExtractTargets(
                    DepartmentNoticeContentStatus.SUCCESS,
                    List.of(
                            DepartmentNoticeScheduleExtractStatus.PENDING,
                            DepartmentNoticeScheduleExtractStatus.FAILED
                    ),
                    PageRequest.of(0, EXTRACT_LIMIT_PER_RUN)
            );

            if (notices.isEmpty()) {
                if (started) {
                    log.info("학과 공지 AI 일정 추출을 완료했습니다. processedCount={}, successCount={}, noScheduleCount={}, failedCount={}",
                            processedCount, successCount, noScheduleCount, failedCount);
                }
                return;
            }

            if (!started) {
                started = true;
                log.info("학과 공지 AI 일정 추출을 시작합니다. count={}", notices.size());
            }

            for (DepartmentNotice notice : notices) {
                if (!isWithinExtractWindow()) {
                    log.info("학과 공지 AI 일정 추출 가능 시간이 종료되어 작업을 중단합니다. processedCount={}, successCount={}, noScheduleCount={}, failedCount={}",
                            processedCount, successCount, noScheduleCount, failedCount);
                    return;
                }

                DepartmentNoticeScheduleExtractStatus status = extractSchedule(notice);
                processedCount++;

                if (status == DepartmentNoticeScheduleExtractStatus.SUCCESS) {
                    successCount++;
                } else if (status == DepartmentNoticeScheduleExtractStatus.NO_SCHEDULE) {
                    noScheduleCount++;
                } else if (status == DepartmentNoticeScheduleExtractStatus.FAILED) {
                    failedCount++;
                }
            }

            if (!pauseBetweenBatches()) {
                log.info("학과 공지 AI 일정 추출 대기 중 인터럽트가 발생해 작업을 중단합니다. processedCount={}, successCount={}, noScheduleCount={}, failedCount={}",
                        processedCount, successCount, noScheduleCount, failedCount);
                return;
            }
        }
    }

    private DepartmentNoticeScheduleExtractStatus extractSchedule(DepartmentNotice departmentNotice) {
        String requestBody = buildRequestBody(departmentNotice);
        if (isBlank(requestBody)) {
            persistenceService.markNoSchedule(departmentNotice.getId(), "");
            log.info("학과 공지 AI 일정 추출을 건너뜁니다. noticeId={}, department={}, reason={}",
                    departmentNotice.getId(), departmentNotice.getDepartment().name(), "empty_request_body");
            return DepartmentNoticeScheduleExtractStatus.NO_SCHEDULE;
        }

        persistenceService.markProcessing(departmentNotice.getId());
        log.info("학과 공지 AI 일정 추출 요청을 시작합니다. noticeId={}, department={}, requestLength={}, url={}",
                departmentNotice.getId(),
                departmentNotice.getDepartment().name(),
                requestBody.length(),
                departmentNotice.getUrl());

        try {
            DepartmentNoticeScheduleExtractResponse response = requestScheduleExtract(requestBody);
            validateResponse(response);

            String responseJson = writeJson(response);
            int responseCount = response.getData() == null ? 0 : response.getData().size();

            log.info("학과 공지 AI 일정 추출 응답을 받았습니다. noticeId={}, department={}, status={}, count={}, responseLength={}, url={}",
                    departmentNotice.getId(),
                    departmentNotice.getDepartment().name(),
                    response.getStatus(),
                    responseCount,
                    responseJson.length(),
                    departmentNotice.getUrl());

            List<Schedule> schedules = buildSchedules(departmentNotice, response);

            if (response.getData() == null || response.getData().isEmpty()) {
                persistenceService.markNoSchedule(departmentNotice.getId(), responseJson);
                log.info("학과 공지 AI 일정 추출 결과 일정이 없습니다. noticeId={}, department={}, url={}",
                        departmentNotice.getId(), departmentNotice.getDepartment().name(), departmentNotice.getUrl());
                return DepartmentNoticeScheduleExtractStatus.NO_SCHEDULE;
            }

            if (schedules.isEmpty()) {
                throw new IllegalStateException("AI 일정 추출 응답에서 저장 가능한 일정 날짜를 찾지 못했습니다.");
            }

            log.info("학과 공지 AI 일정 저장을 시작합니다. noticeId={}, department={}, scheduleCount={}, url={}",
                    departmentNotice.getId(),
                    departmentNotice.getDepartment().name(),
                    schedules.size(),
                    departmentNotice.getUrl());

            persistenceService.saveSuccess(departmentNotice.getId(), responseJson, schedules);

            log.info("학과 공지 AI 일정 저장을 완료했습니다. noticeId={}, department={}, scheduleCount={}, url={}",
                    departmentNotice.getId(),
                    departmentNotice.getDepartment().name(),
                    schedules.size(),
                    departmentNotice.getUrl());
            return DepartmentNoticeScheduleExtractStatus.SUCCESS;
        } catch (Exception e) {
            persistenceService.markFailed(departmentNotice.getId(), limitMessage(e.getMessage()));
            log.warn("학과 공지 AI 일정 추출에 실패했습니다. noticeId={}, department={}, url={}, reason={}",
                    departmentNotice.getId(), departmentNotice.getDepartment().name(), departmentNotice.getUrl(), e.getMessage());
            return DepartmentNoticeScheduleExtractStatus.FAILED;
        }
    }

    private DepartmentNoticeScheduleExtractResponse requestScheduleExtract(String requestBody) {
        Duration timeout = Duration.ofSeconds(Math.max(timeoutSeconds, 30));

        return webClient.post()
                .uri(buildExtractUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new IllegalStateException(
                                "AI 일정 추출 API 호출이 실패했습니다. status="
                                        + clientResponse.statusCode().value()
                                        + ", body=" + limitMessage(body)
                        )))
                .bodyToMono(DepartmentNoticeScheduleExtractResponse.class)
                .timeout(timeout)
                .onErrorMap(TimeoutException.class,
                        e -> new IllegalStateException("AI 일정 추출 응답 시간이 초과되었습니다. timeoutSeconds=" + timeoutSeconds, e))
                .block(timeout.plusSeconds(5));
    }

    private void validateResponse(DepartmentNoticeScheduleExtractResponse response) {
        if (response == null) {
            throw new IllegalStateException("AI 일정 추출 응답이 비어 있습니다.");
        }
        if (!"success".equalsIgnoreCase(response.getStatus())) {
            throw new IllegalStateException("AI 일정 추출 응답 상태가 올바르지 않습니다. status=" + response.getStatus());
        }
    }

    private List<Schedule> buildSchedules(
            DepartmentNotice departmentNotice,
            DepartmentNoticeScheduleExtractResponse response
    ) {
        if (response.getData() == null || response.getData().isEmpty()) {
            return List.of();
        }

        long nextId = scheduleRepository.findMaxId();
        Set<String> seenKeys = new LinkedHashSet<>();
        List<Schedule> schedules = new ArrayList<>();

        for (DepartmentNoticeScheduleExtractItem item : response.getData()) {
            DateRange dateRange;
            try {
                dateRange = resolveDateRange(item);
            } catch (Exception e) {
                log.warn("학과 공지 AI 일정 날짜를 해석하지 못했습니다. noticeId={}, department={}, title={}, reason={}",
                        departmentNotice.getId(),
                        departmentNotice.getDepartment().name(),
                        normalizeText(item.getTitle()),
                        e.getMessage());
                continue;
            }

            if (dateRange == null) {
                log.warn("학과 공지 AI 일정 날짜가 비어 있어 저장을 건너뜁니다. noticeId={}, department={}, title={}",
                        departmentNotice.getId(),
                        departmentNotice.getDepartment().name(),
                        normalizeText(item.getTitle()));
                continue;
            }

            String title = normalizeText(item.getTitle());
            if (title.isBlank()) {
                title = normalizeText(departmentNotice.getTitle());
            }

            String dedupeKey = title + "|" + dateRange.start() + "|" + dateRange.end();
            if (!seenKeys.add(dedupeKey)) {
                continue;
            }

            schedules.add(Schedule.builder()
                    .id(++nextId)
                    .startDate(dateRange.start())
                    .endDate(dateRange.end())
                    .content(title)
                    .description(normalizeText(item.getDescription()))
                    .department(departmentNotice.getDepartment())
                    .sourceNoticeId(departmentNotice.getId())
                    .aiGenerated(true)
                    .build());
        }

        return schedules;
    }

    private DateRange resolveDateRange(DepartmentNoticeScheduleExtractItem item) {
        LocalDate startDate = parseDate(item.getStartDate());
        LocalDate endDate = parseDate(item.getEndDate());

        if (startDate == null && endDate == null) {
            return null;
        }

        LocalDate normalizedStart = startDate != null ? startDate : endDate;
        LocalDate normalizedEnd = endDate != null ? endDate : startDate;

        if (normalizedStart.isAfter(normalizedEnd)) {
            return new DateRange(normalizedEnd, normalizedStart);
        }

        return new DateRange(normalizedStart, normalizedEnd);
    }

    private LocalDate parseDate(String value) {
        String normalized = normalizeText(value);
        if (normalized.isBlank() || "null".equalsIgnoreCase(normalized)) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("잘못된 날짜 형식입니다. value=" + normalized, e);
        }
    }

    private String buildRequestBody(DepartmentNotice departmentNotice) {
        List<String> sections = new ArrayList<>();
        String title = normalizeText(departmentNotice.getTitle());
        String contentText = normalizeText(departmentNotice.getContentText());
        String attachmentText = normalizeText(departmentNotice.getAttachmentText());
        String ocrText = normalizeText(departmentNotice.getOcrText());
        String createDate = departmentNotice.getCreateDate() != null ? departmentNotice.getCreateDate().toString() : "";
        boolean usedBestEffortForContent = false;

        if (contentText.isBlank()) {
            contentText = normalizeText(departmentNotice.getBestEffortText());
            usedBestEffortForContent = true;
        }

        appendSection(sections, "[제목]", title);
        appendSection(sections, "[작성일]", createDate);
        appendSection(sections, "[내용]", contentText);

        if (!usedBestEffortForContent) {
            appendSection(sections, "[첨부파일 텍스트]", attachmentText);
            appendSection(sections, "[OCR 텍스트]", ocrText);
        }

        return String.join("\n\n", sections).trim();
    }

    private void appendSection(List<String> sections, String label, String value) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return;
        }
        sections.add(label + "\n" + normalized);
    }

    private String buildExtractUri() {
        return trimTrailingSlash(baseUrl) + EXTRACT_PATH;
    }

    private String trimTrailingSlash(String value) {
        String normalized = normalizeText(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isWithinExtractWindow() {
        LocalTime now = LocalTime.now(EXTRACT_ZONE_ID);
        return !now.isBefore(EXTRACT_WINDOW_START) && now.isBefore(EXTRACT_WINDOW_END);
    }

    private boolean pauseBetweenBatches() {
        try {
            Thread.sleep(BATCH_DELAY_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean isConfigured() {
        return !isBlank(baseUrl) && !isBlank(apiKey);
    }

    private boolean isBlank(String value) {
        return normalizeText(value).isBlank();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("AI 일정 추출 응답 직렬화에 실패했습니다.", e);
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\u00A0", " ").trim();
    }

    private String limitMessage(String message) {
        String normalized = normalizeText(message);
        if (normalized.length() <= ERROR_MESSAGE_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, ERROR_MESSAGE_LIMIT);
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
