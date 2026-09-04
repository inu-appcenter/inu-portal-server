package kr.inuappcenterportal.inuportal.domain.firebase.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.AdminNotificationRequest;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.ScheduledNotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.model.ScheduledNotification;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.ScheduledNotificationRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScheduledNotificationService {

    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final ScheduledNotificationTransactionService txService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /** 예약 가능한 최대 기간(일). 오타로 지나치게 먼 미래가 예약돼 발송 대상 조회 쿼리의
     * due 인덱스에 장기간 눌러앉는 것을 막는다. */
    @Value("${fcm.schedule.max-advance-days:90}")
    private long maxAdvanceDays;

    @Transactional
    public ScheduledNotification reserve(AdminNotificationRequest request) {
        LocalDateTime scheduledAt = request.scheduledAt();
        if (scheduledAt == null) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (scheduledAt.isAfter(now.plusDays(maxAdvanceDays))) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        String requestPayload;
        try {
            requestPayload = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }

        ScheduledNotification scheduledNotification = ScheduledNotification.builder()
                .title(request.title())
                .content(request.content())
                .path(request.path())
                .targetType(request.resolveTargetType())
                .subFilter(request.resolveSubFilter())
                .requestPayload(requestPayload)
                .scheduledAt(scheduledAt)
                .build();

        return scheduledNotificationRepository.save(scheduledNotification);
    }

    public void cancel(Long id) {
        scheduledNotificationRepository.findById(id)
                .orElseThrow(() -> new MyException(MyErrorCode.MESSAGE_NOT_FOUND));

        if (!txService.cancelIfScheduled(id)) {
            // 이미 발송 절차가 시작됐거나(DISPATCHING 이후) 종결된 건.
            throw new MyException(MyErrorCode.SCHEDULED_NOTIFICATION_ALREADY_DISPATCHING);
        }
    }

    @Transactional(readOnly = true)
    public ListResponseDto<ScheduledNotificationResponse> findScheduled(int page) {
        Pageable pageable = PageRequest.of(page - 1, 20, Sort.by(Sort.Direction.DESC, "id"));
        Page<ScheduledNotification> result = scheduledNotificationRepository.findAllByOrderByScheduledAtDesc(pageable);
        return ListResponseDto.of(
                result.getTotalPages(),
                result.getTotalElements(),
                result.map(ScheduledNotificationResponse::of).getContent()
        );
    }
}
