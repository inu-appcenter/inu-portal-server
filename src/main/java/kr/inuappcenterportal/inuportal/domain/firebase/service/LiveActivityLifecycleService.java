package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.DueLiveActivity;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.LiveActivityInstanceRequestDto;
import kr.inuappcenterportal.inuportal.domain.firebase.model.LiveActivityInstance;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.LiveActivityInstanceRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * iOS 시간표 Live Activity의 등록과 갱신/종료 상태 기록. 발송은 {@code LiveActivityLifecycleScheduler}가
 * 트랜잭션 밖에서 하고, 여기서는 짧은 트랜잭션으로 읽고 쓰기만 한다.
 */
@Service
@RequiredArgsConstructor
public class LiveActivityLifecycleService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LiveActivityInstanceRepository liveActivityInstanceRepository;

    /**
     * 앱이 보고한 Activity와 그 업데이트 토큰을 저장한다. 같은 Activity는 갱신한다.
     * 로그인 전용이다({@code PUT /api/tokens/live-activity/activities}는 authenticated).
     */
    @Transactional
    public void register(LiveActivityInstanceRequestDto requestDto, Long memberId) {
        Objects.requireNonNull(memberId, "memberId");
        JsonNode props = readProps(requestDto.getProps());
        long startAt = props.path("startTimestamp").asLong(0);
        long endAt = props.path("endTimestamp").asLong(0);
        if (startAt <= 0 || endAt <= startAt) {
            throw new MyException(MyErrorCode.INVALID_LIVE_ACTIVITY_PROPS);
        }

        liveActivityInstanceRepository.findByActivityId(requestDto.getActivityId())
                .ifPresentOrElse(
                        instance -> instance.refresh(memberId, requestDto.getToken(), requestDto.getPushToken(),
                                requestDto.getProps(), startAt, endAt),
                        () -> liveActivityInstanceRepository.save(LiveActivityInstance.builder()
                                .memberId(memberId)
                                .activityId(requestDto.getActivityId())
                                .fcmToken(requestDto.getToken())
                                .pushToken(requestDto.getPushToken())
                                .propsJson(requestDto.getProps())
                                .startAt(startAt)
                                .endAt(endAt)
                                .build()));
    }

    /** 수업이 시작돼 "수업 중" 갱신이나 종료가 필요할 수 있는 Activity. */
    @Transactional(readOnly = true)
    public List<DueLiveActivity> findDue(long nowMs) {
        return liveActivityInstanceRepository.findAllByEndedFalseAndStartAtLessThanEqual(nowMs).stream()
                .map(i -> new DueLiveActivity(i.getId(), i.getFcmToken(), i.getPushToken(), i.getPropsJson(),
                        i.getStartAt(), i.getEndAt(), i.isOngoingSent()))
                .toList();
    }

    /** 이 기기들에 떠 있는 Activity 중 {@code classStartMs}보다 먼저 시작한 수업의 것. */
    @Transactional(readOnly = true)
    public List<DueLiveActivity> findEarlierActive(Long memberId, Collection<String> fcmTokens, long classStartMs) {
        if (memberId == null || fcmTokens.isEmpty()) {
            return List.of();
        }
        return liveActivityInstanceRepository
                .findAllByMemberIdAndEndedFalseAndFcmTokenInAndStartAtLessThan(memberId, fcmTokens, classStartMs)
                .stream()
                .map(i -> new DueLiveActivity(i.getId(), i.getFcmToken(), i.getPushToken(), i.getPropsJson(),
                        i.getStartAt(), i.getEndAt(), i.isOngoingSent()))
                .toList();
    }

    @Transactional
    public void markOngoingSent(Long id) {
        liveActivityInstanceRepository.findById(id).ifPresent(LiveActivityInstance::markOngoingSent);
    }

    @Transactional
    public void markEnded(Long id) {
        liveActivityInstanceRepository.findById(id).ifPresent(LiveActivityInstance::markEnded);
    }

    @Transactional
    public int deleteEndedBefore(long beforeMs) {
        return liveActivityInstanceRepository.deleteEndedBefore(beforeMs);
    }

    /** props의 phase만 바꾼 JSON. 레이아웃은 phase로 "곧 수업"/"수업 중"을 고른다. */
    public static String withPhase(String propsJson, String phase) {
        JsonNode props = readProps(propsJson);
        ((ObjectNode) props).put("phase", phase);
        return props.toString();
    }

    private static JsonNode readProps(String propsJson) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(propsJson);
            if (node == null || !node.isObject()) {
                throw new MyException(MyErrorCode.INVALID_LIVE_ACTIVITY_PROPS);
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new MyException(MyErrorCode.INVALID_LIVE_ACTIVITY_PROPS);
        }
    }
}
