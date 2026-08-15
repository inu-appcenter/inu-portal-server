package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableEvaluation.TimeTableEvaluationResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableEvaluation;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableEvaluationRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatMessageDto;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatRequestDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.VllmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeTableEvaluationService {

    public static final int MAX_REGENERATE_COUNT = 3;

    private final TimeTableRepository timeTableRepository;
    private final TimeTableEvaluationRepository timeTableEvaluationRepository;
    private final TimeTableService timeTableService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final VllmService vllmService;

    /**
     * 캐시된 평가가 있는지 조회 (캐시가 유효하면 반환, 없거나 변경되었으면 null 반환)
     */
    @Transactional(readOnly = true)
    public TimeTableEvaluationResponseDto getCachedEvaluation(Long memberId, Long timeTableId) {
        TimeTableDetailResponseDto detail = timeTableService.getTimeTableDetail(memberId, timeTableId);
        String currentHash = TimeTableAnalysisUtils.calculateHash(detail.items());

        Optional<TimeTableEvaluation> evaluationOpt = timeTableEvaluationRepository.findByTimeTableId(timeTableId);
        if (evaluationOpt.isPresent()) {
            TimeTableEvaluation eval = evaluationOpt.get();
            if (Objects.equals(eval.getTimetableHash(), currentHash)) {
                return TimeTableEvaluationResponseDto.of(eval, true);
            }
        }
        return null;
    }

    /**
     * 시간표 평가 SSE 스트리밍
     */
    @Transactional
    public SseEmitter streamEvaluation(Long memberId, Long timeTableId, boolean forceRefresh) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new MyException(MyErrorCode.TIMETABLE_NOT_FOUND));

        if (!timeTable.getMember().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.NOT_READABLE_TIMETABLE);
        }

        TimeTableDetailResponseDto detail = timeTableService.getTimeTableDetail(memberId, timeTableId);

        if (detail.items() == null || detail.items().isEmpty()) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        String currentHash = TimeTableAnalysisUtils.calculateHash(detail.items());
        Optional<TimeTableEvaluation> existingEvaluationOpt = timeTableEvaluationRepository.findByTimeTableId(timeTableId);

        // SSE Emitter 생성 (타임아웃 2분)
        SseEmitter emitter = new SseEmitter(120_000L);
        emitter.onCompletion(() -> log.debug("SSE completed for timetable {}", timeTableId));
        emitter.onTimeout(() -> {
            log.warn("SSE timeout for timetable {}", timeTableId);
            emitter.complete();
        });
        emitter.onError(e -> log.debug("SSE error for timetable {}: {}", timeTableId, e.getMessage()));

        // 1. 강제 새로고침(forceRefresh) 시 재생성 횟수 제한 검증
        if (forceRefresh && existingEvaluationOpt.isPresent()) {
            TimeTableEvaluation existing = existingEvaluationOpt.get();
            boolean isHashSame = Objects.equals(existing.getTimetableHash(), currentHash);
            // 동일한 시간표 해시 상태에서 3회를 이미 초과한 경우
            if (isHashSame && existing.getRegenerateCount() >= MAX_REGENERATE_COUNT) {
                CompletableFuture.runAsync(() -> {
                    try {
                        sendSseEvent(emitter, "error", Map.of(
                                "message", "동일한 시간표에서는 최대 3회까지만 다시 생성할 수 있습니다. 시간표를 수정하면 새롭게 평가받을 수 있어요!",
                                "code", "TIMETABLE_EVALUATION_LIMIT_EXCEEDED",
                                "regenerateCount", existing.getRegenerateCount(),
                                "remainingCount", 0
                        ));
                    } finally {
                        emitter.complete();
                    }
                });
                return emitter;
            }
        }

        // 2. 캐시 확인 (강제 새로고침이 아니고, 해시가 일치하는 경우)
        if (!forceRefresh && existingEvaluationOpt.isPresent()) {
            TimeTableEvaluation existing = existingEvaluationOpt.get();
            if (Objects.equals(existing.getTimetableHash(), currentHash)) {
                int regCount = existing.getRegenerateCount();
                int remaining = Math.max(0, MAX_REGENERATE_COUNT - regCount);
                CompletableFuture.runAsync(() -> {
                    try {
                        sendSseEvent(emitter, "start", Map.of(
                                "isCached", true,
                                "timetableHash", currentHash,
                                "regenerateCount", regCount,
                                "remainingCount", remaining
                        ));
                        sendSseEvent(emitter, "delta", Map.of("content", existing.getContent()));
                        sendSseEvent(emitter, "done", Map.of(
                                "status", "SUCCESS",
                                "isCached", true,
                                "regenerateCount", regCount,
                                "remainingCount", remaining
                        ));
                        emitter.complete();
                    } catch (Exception e) {
                        log.debug("Error sending cached SSE: {}", e.getMessage());
                        emitter.complete();
                    }
                });
                return emitter;
            }
        }

        // 3. 과목 개설 정보(전공/교양/이러닝) 조회
        List<Long> offeringIds = detail.items().stream()
                .filter(item -> item.course() != null && item.course().courseOfferingId() != null)
                .map(item -> item.course().courseOfferingId())
                .toList();

        Map<Long, CourseOffering> offeringMap = courseOfferingRepository.findAllById(offeringIds).stream()
                .collect(Collectors.toMap(CourseOffering::getId, o -> o));

        // 4. 시간표 메트릭 분석 및 프롬프트 생성
        TimeTableAnalysisUtils.TimetableSummary summary = TimeTableAnalysisUtils.analyzeTimetable(detail.items(), offeringMap);
        List<VllmChatMessageDto> messages = buildPromptMessages(timeTable.getTimeTableName(), summary);

        String model = vllmService.getModel();
        VllmChatRequestDto chatRequest = VllmChatRequestDto.of(model, messages, true);

        StringBuilder fullContentAccumulator = new StringBuilder();

        // 새로 생성할 때의 예상 재생성 카운트 계산
        int currentRegCount = 0;
        if (existingEvaluationOpt.isPresent()) {
            TimeTableEvaluation existing = existingEvaluationOpt.get();
            boolean isHashSame = Objects.equals(existing.getTimetableHash(), currentHash);
            if (isHashSame && forceRefresh) {
                currentRegCount = existing.getRegenerateCount() + 1;
            }
        }
        int remainingCount = Math.max(0, MAX_REGENERATE_COUNT - currentRegCount);

        sendSseEvent(emitter, "start", Map.of(
                "isCached", false,
                "timetableHash", currentHash,
                "regenerateCount", currentRegCount,
                "remainingCount", remainingCount
        ));

        int finalCurrentRegCount = currentRegCount;
        int finalRemainingCount = remainingCount;

        vllmService.streamChat(
                chatRequest,
                token -> {
                    fullContentAccumulator.append(token);
                    sendSseEvent(emitter, "delta", Map.of("content", token));
                },
                () -> {
                    String fullContent = fullContentAccumulator.toString().trim();
                    if (!fullContent.isEmpty()) {
                        saveOrUpdateEvaluation(timeTableId, fullContent, currentHash);
                    }
                    sendSseEvent(emitter, "done", Map.of(
                            "status", "SUCCESS",
                            "isCached", false,
                            "regenerateCount", finalCurrentRegCount,
                            "remainingCount", finalRemainingCount
                    ));
                    emitter.complete();
                },
                error -> {
                    log.error("vLLM streaming error for timetable {}", timeTableId, error);
                    sendSseEvent(emitter, "error", Map.of("message", "AI 평가 생성 중 오류가 발생했습니다."));
                    emitter.complete();
                }
        );

        return emitter;
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE send failed (client disconnected): {}", e.getMessage());
        }
    }

    @Transactional
    public void saveOrUpdateEvaluation(Long timeTableId, String content, String hash) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId).orElse(null);
        if (timeTable == null) return;

        Optional<TimeTableEvaluation> evalOpt = timeTableEvaluationRepository.findByTimeTableId(timeTableId);
        if (evalOpt.isPresent()) {
            TimeTableEvaluation eval = evalOpt.get();
            boolean isHashChanged = !Objects.equals(eval.getTimetableHash(), hash);
            eval.updateContent(content, hash, isHashChanged);
            timeTableEvaluationRepository.save(eval);
        } else {
            TimeTableEvaluation newEval = TimeTableEvaluation.create(timeTable, content, hash);
            timeTableEvaluationRepository.save(newEval);
        }
    }

    private List<VllmChatMessageDto> buildPromptMessages(String tableName, TimeTableAnalysisUtils.TimetableSummary summary) {
        String systemPrompt = """
                너는 인천대학교의 활기차고 센스 넘치는 마스코트 캐릭터 '횃불이'야!
                인천대학교 학생의 이번 학기 시간표를 꼼꼼히 살펴보고 현실감 넘치면서도 재미있게 코칭/평가해주는 역할을 맡았어.
                
                [페르소나 & 톤앤매너 규칙]
                1. 말투는 친근하고 다정한 대학생 친구 말투 (~했어?, ~잖아!, ~인걸?, ㅠㅠ, ㅎㅎ, 😱, 🔥, 🍯 등 이모지 적절히 사용).
                2. 시간표가 아쉽거나 힘든 구성(우주공강, 월~금 매일 등교, 1교시 9시 연타, 전공 과목 폭탄, 점심 굶는 연강 등)일 경우:
                   - "저런... ㅠㅠ", "이건 전설의 고통 시간표?!" 하며 살짝 장난스럽게 놀리거나 위로해줘. 절대 비하하지 말고 유쾌하고 따뜻하게 위로와 생존 꿀팁을 전해줘.
                3. 시간표가 훌륭한 구성(금공강/월공강 확보, 이러닝/온라인 꿀과목 포함, 12시~1시 점심시간 완벽, 적절한 연강 밸런스 등)일 경우:
                   - "와 대박! 신의 손이야?!", "금토일 3일 연휴라니 너무 부럽다~" 하며 극찬과 부러움을 표현해줘.
                4. 과목 구성(전공, 교양, 이러닝) 분석 반영:
                   - 전공 과목이 많으면(예: 4개 이상): 과제와 시험기간의 험난한 여정을 팩폭하고 체력 관리 응원하기.
                   - 이러닝(온라인) 과목이 있으면: 통학 부담을 줄여주는 현명한 선택인지 칭찬하거나, 출석/과제 마감 기한 깜빡하지 말라고 당부하기.
                5. 답변은 가독성이 좋게 마크다운(Markdown) 포맷으로 작성해줘:
                   - **한 줄 요약 칭호**: 예) 🔥 `[월요병과 우주공강의 콜라보]` or 🍯 `[주4일 꿀벌 라이프]`
                   - **시간표 등급 & 점수**: 예) `S급 (92점)` or `C+ (58점)`
                   - **횃불이의 팩트 체크**: 시간표의 최고 장점과 치명적 주의점
                   - **횃불이의 생존 꿀팁**: 인천대 캠퍼스 생활 맞춤 조언 (학식, 도서관 낮잠, 카페인 충전 등)
                6. 전체 길이는 너무 길지 않게 4~6개 단락 정도로 깔끔하고 임팩트 있게 작성해줘.
                """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append(String.format("시간표 이름: %s\n", tableName));
        userPrompt.append(String.format("총 학점: %d학점 (총 과목/일정 수: %d개)\n", summary.totalCredits(), summary.totalClasses()));
        userPrompt.append(String.format("- 전공 과목 수: %d개\n", summary.majorCourseCount()));
        userPrompt.append(String.format("- 교양 과목 수: %d개\n", summary.generalCourseCount()));
        userPrompt.append(String.format("- 이러닝(온라인) 과목 수: %d개\n", summary.onlineCourseCount()));
        if (summary.otherCourseCount() > 0) {
            userPrompt.append(String.format("- 기타/커스텀 일정 수: %d개\n", summary.otherCourseCount()));
        }
        userPrompt.append(String.format("공강 요일: %s\n", summary.freeDays().isEmpty() ? "없음 (주 5일 등교)" : String.join(", ", summary.freeDays())));
        userPrompt.append(String.format("오전 9시(1교시) 시작 수업 횟수: 주 %d회\n", summary.countOf9AmClasses()));
        userPrompt.append(String.format("2시간 이상 우주공강 횟수: 총 %d회\n", summary.totalLongGapsCount()));
        userPrompt.append("\n[상세 요일별 일정]:\n");
        userPrompt.append(summary.rawScheduleText());
        userPrompt.append("\n위 시간표를 분석해서 횃불이의 개성 넘치고 솔직한 평가를 들려줘!");

        return List.of(
                VllmChatMessageDto.system(systemPrompt),
                VllmChatMessageDto.user(userPrompt.toString())
        );
    }
}
