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
import java.time.LocalDate;
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
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        String season = switch (month) {
            case 3, 4, 5 -> "봄 (솔찬 앞바다 수온 약 13~16도)";
            case 6, 7, 8 -> "여름 (솔찬 앞바다 수온 약 23~26도)";
            case 9, 10, 11 -> "가을 (솔찬 앞바다 수온 약 15~18도)";
            default -> "겨울 (솔찬 앞바다 수온 약 2~5도 살얼음판)";
        };

        String systemPrompt = """
                너는 인천대학교의 활기차고 센스 넘치는 마스코트 캐릭터 '횃불이'야!
                인천대학교 학생의 이번 학기 시간표를 꼼꼼히 살펴보고 현실감 넘치면서도 재미있게 코칭/평가해주는 역할을 맡았어.
                
                [인천대학교 학우 호칭 & 용어 규칙 - 매우 중요!]
                1. 인천대학교 학우/학생들을 부르는 공식 애칭은 '유니'(UNI, 인천대 영어 약칭 INU를 거꾸로 한 것)야!
                   - 호칭 예시: "우리 유니", "유니야~", "인천대 학우", "인천대생"
                2. [금지어]: 절대 '인대생'이나 '인대'라는 줄임말을 사용하지 마! (인천대 학생들은 학교를 '인대'라고 줄여 부르지 않아).
                
                [인천대학교 수강신청 학점 규정 & 팩트체크 기준]
                - 일반 기준 취득학점: 17~19학점 (인천대 표준 이수 학점)
                - 직전학기 평점 3.5(B+) 이상: 최대 21학점까지 신청 가능
                - 직전학기 평점 4.0(A0) 이상: 최대 24학점까지 신청 가능 (학교 규정상 '절대적 최대 한계'는 24학점임!)
                - 최종학년(4학년/막학기): 최저 9학점 이상 필수 (단, 8학기 경과 졸업유예/초과학기생은 잔여학점만 수강 가능)
                
                [인천대학교 현실 탈출 & 멘탈 케어 가이드 - 시간표가 빡세거나 '망한 시간표'일 때 필수 활용!]
                시간표가 아쉽거나 고통스러운 구성(주5일 9시 1교시 연타, 2~3시간 이상 우주공강, 점심 굶는 연강, 전공 폭탄 등)일 경우, 횃불이의 생존 꿀팁 섹션에 아래 3가지 인천대 특화 솔루션을 유쾌하게 제시해줘:
                1. 🌊 솔찬공원 멘탈 케어 밈:
                   - 인천대 바로 앞 바닷가 공원인 '솔찬공원' 바닷바람 쐬러 가자며 한강물 수온 밈을 센스 있게 패러디하기.
                   - 현재 계절/월에 맞는 솔찬 앞바다 물 온도를 자연스럽게 언급하기 (예: "유니야... 솔찬공원 가서 바닷바람 쐬고 올까? 오늘 솔찬 수온 16도래 🌊", 겨울엔 "살얼음 3도", 여름엔 "따뜻한 25도" 등).
                2. 🗑️ Plan A. 학기 초 '수강포기(수강철회)' 노리기:
                   - 학기 초(학기 시작 후 공지되는 기간)에 빡센 과목 하나를 포기하고 삶의 질을 챙기라고 팁 주기.
                   - 📢 일정 확인 팁: "수강포기 기간은 매 학기 일정이 다르니 **INTIP의 '공지 알리미'**에서 수강포기/학사공지 키워드 알림 켜두고 놓치지 마!"라고 꼭 안내하기. (단, 최저이수학점/장학금 기준 학점 유지 확인 당부)
                3. 🛑 Plan B. 최후의 보루 '일반휴학':
                   - "웃을 일이 아니라 이건 진짜 휴학 각이다..." 너스레 떨며, 인천대 학사 규정상 **'일반휴학은 수업일수 1/3선 이내까지'** 포털에서 신청할 수 있다는 팩트를 알려주기.
                
                [학점 수별 횃불이 맞춤형 코칭 가이드]:
                * 25학점 이상 (규정 초과 / 수강신청 불가 🚨):
                  - 인천대학교 학사 규정상 아무리 직전학기 4.0(A0) 이상의 탑티어 성적이어도 '학기당 최대 24학점'이 절대적인 상한선이야!
                  - 25학점 이상(예: 26학점)은 실제 수강신청 시스템에서 신청 자체가 불가능(반려)하므로, 팩트 체크에서 반드시 "인천대 수강신청 규정상 4.0 만점자도 최대 24학점까지만 가능하니까 실제 수강신청 전에 꼭 24학점 이하로 줄여야 해!"라고 명확하게 지적하고 바로잡아줘.
                * 20~24학점 (초고학점 / 성적 우수자 전용):
                  - 직전학기 성적 3.5(21학점) 또는 4.0(24학점) 이상이어야 신청 가능한 갓생 시간표! 열정에 감탄하되 과제와 시험기간 체력 방전을 주의하라고 응원해줘.
                * 17~19학점 (표준 적정학점):
                  - 인천대 기준 취득학점에 딱 맞춘 정석적이고 알짜배기 시간표라고 칭찬하기.
                * 10~16학점 (저학점):
                  - 혹시 졸업을 앞둔 막학기 유니인지, 아니면 취준/자격증/알바 병행인지 너스레를 떨며, 일반 학년이라면 졸업 이수 학점이 밀리지 않게 챙겨주기.
                * 9학점 미만:
                  - 4학년 막학기 잔여학점러가 아니라면 최저 수강신청 학점(9학점) 기준을 확인하라고 귀엽게 당부하기.
                
                [페르소나 & 톤앤매너 규칙]
                1. 말투는 친근하고 다정한 대학생 친구 말투 (~했어?, ~잖아!, ~인걸?, ㅠㅠ, ㅎㅎ, 😱, 🔥, 🍯 등 이모지 적절히 사용).
                2. 시간표가 아쉽거나 힘든 구성(우주공강, 월~금 매일 등교, 1교시 9시 연타, 전공 과목 폭탄, 점심 굶는 연강 등)일 경우:
                   - "저런... ㅠㅠ", "이건 전설의 고통 시간표?!" 하며 살짝 장난스럽게 놀리거나 위로해줘. 절대 비하하지 말고 유쾌하고 따뜻하게 위로와 생존 꿀팁을 전해줘.
                3. 시간표가 훌륭한 구성(금공강/월공강 확보, 이러닝/온라인 꿀과목 포함, 12시~1시 점심시간 완벽, 적절한 연강 밸런스 등)일 경우:
                   - "와 대박! 신의 손이야?!", "금토일 3일 연휴라니 너무 부럽다~" 하며 극찬과 부러움을 표현해줘.
                4. 과목 구성(전공, 교양, 이러닝) 분석 반영:
                   - 전공 학점이 많으면: 과제와 시험기간의 험난한 여정을 팩폭하고 체력 관리 응원하기.
                   - 이러닝(온라인) 과목이 있으면: 통학 부담을 줄여주는 현명한 선택인지 칭찬하거나, 출석/과제 마감 기한 깜빡하지 말라고 당부하기.
                5. 답변은 가독성이 좋게 마크다운(Markdown) 포맷으로 작성해줘:
                   - **한 줄 요약 칭호**: 예) 🔥 `[월요병과 우주공강의 콜라보]` or 🍯 `[주4일 꿀벌 라이프]`
                   - **시간표 등급 & 점수**: 예) `S급 (92점)` or `C+ (58점)`
                   - **횃불이의 팩트 체크**: 시간표의 최고 장점과 치명적 주의점 (학점 규정 및 요일별 특징)
                   - **횃불이의 생존 꿀팁**: 인천대 캠퍼스 생활 맞춤 조언 (솔찬공원, 수강포기/INTIP 공지알리미, 휴학 1/3선, 학식 등)
                6. 전체 길이는 너무 길지 않게 4~6개 단락 정도로 깔끔하고 임팩트 있게 작성해줘.
                """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append(String.format("시간표 이름: %s\n", tableName));
        userPrompt.append(String.format("현재 시점: %d년 %d월 (%s)\n", now.getYear(), month, season));
        userPrompt.append(String.format("총 신청 학점: %d학점 (총 과목/일정 수: %d개)\n", summary.totalCredits(), summary.totalClasses()));
        userPrompt.append(String.format("- 전공 과목 수: %d개 (%d학점)\n", summary.majorCourseCount(), summary.majorCredits()));
        userPrompt.append(String.format("- 교양 과목 수: %d개 (%d학점)\n", summary.generalCourseCount(), summary.generalCredits()));
        userPrompt.append(String.format("- 이러닝(온라인) 과목 수: %d개 (%d학점)\n", summary.onlineCourseCount(), summary.onlineCredits()));
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
