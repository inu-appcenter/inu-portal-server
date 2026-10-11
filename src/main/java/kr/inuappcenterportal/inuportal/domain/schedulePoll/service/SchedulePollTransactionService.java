package kr.inuappcenterportal.inuportal.domain.schedulePoll.service;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollErrorCode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollException;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 데이터를 바꾸는 요청(제출, 마감 변경, 종료, 공유)의 공통 처리 순서와 마감 처리.
// 1. 트랜잭션 시작  2. 조율 행 잠금  3. 이미 CLOSED면 409
// 4. 마감이 지났으면 마감 처리를 커밋한 뒤 409  5. revision + 1  6. API별 작업 후 커밋
// (revision을 작업보다 먼저 올려 응답에 커밋될 값이 담기게 한다. 한 트랜잭션이라 밖에서 보는 결과는 같다.)
//
// 4번에서 예외를 트랜잭션 안에서 던지면 마감 처리까지 롤백되므로,
// 트랜잭션은 결과만 돌려주고 커밋이 끝난 뒤에 POLL_CLOSED를 던진다.
@Service
public class SchedulePollTransactionService {

    private static final int MIN_PARTICIPANTS_FOR_RESULT = 2;
    private static final String CLOSED_PUSH_TITLE = "시간 맞추기가 마감됐어요";

    private final TransactionTemplate transactionTemplate;
    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollSubmissionRepository schedulePollSubmissionRepository;
    private final SchedulePollResultService schedulePollResultService;
    private final FcmService fcmService;
    private final Clock clock;

    public SchedulePollTransactionService(PlatformTransactionManager transactionManager,
                                          SchedulePollRepository schedulePollRepository,
                                          SchedulePollSubmissionRepository schedulePollSubmissionRepository,
                                          SchedulePollResultService schedulePollResultService,
                                          FcmService fcmService,
                                          Clock clock) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.schedulePollRepository = schedulePollRepository;
        this.schedulePollSubmissionRepository = schedulePollSubmissionRepository;
        this.schedulePollResultService = schedulePollResultService;
        this.fcmService = fcmService;
        this.clock = clock;
    }

    // 공통 처리 순서에 끼워 넣는 API별 작업
    @FunctionalInterface
    public interface Write<T> {

        // 잠금 직후, 종료 여부 확인보다 먼저 실행한다.
        // 예: 개설자 확인(NOT_HOST), 이미 처리한 clientSubmissionId면 처음 응답 반환.
        // 값을 돌려주면 그 값으로 바로 끝내고 revision은 올리지 않는다.
        default Optional<T> beforeStatusCheck(SchedulePoll poll) {
            return Optional.empty();
        }

        // API별 검증과 변경
        T apply(SchedulePoll poll, LocalDateTime now);
    }

    // 바깥 트랜잭션 안에서 부르면 마감 처리 커밋이 보장되지 않으므로 막는다.
    public <T> T execute(Long pollId, Write<T> write) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("SchedulePollTransactionService는 트랜잭션 밖에서 호출해야 합니다.");
        }
        Outcome<T> outcome = transactionTemplate.execute(status -> {
            SchedulePoll poll = schedulePollRepository.findByIdForUpdate(pollId)
                    .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.POLL_NOT_FOUND));

            Optional<T> early = write.beforeStatusCheck(poll);
            if (early.isPresent()) {
                return Outcome.done(early.get());
            }
            if (poll.isClosed()) {
                throw new SchedulePollException(SchedulePollErrorCode.POLL_CLOSED);
            }
            LocalDateTime now = LocalDateTime.now(clock);
            if (poll.isDeadlinePassed(now)) {
                close(poll, now);
                poll.increaseRevision();
                return Outcome.closed();
            }

            poll.increaseRevision();
            return Outcome.done(write.apply(poll, now));
        });

        if (outcome.closedByDeadline()) {
            throw new SchedulePollException(SchedulePollErrorCode.POLL_CLOSED);
        }
        return outcome.result();
    }

    // 마감 스케줄러용. 마감이 지난 조율 하나를 자기 트랜잭션에서 정리한다.
    // 잠근 뒤 다시 확인해서, 이미 처리됐거나(요청이 먼저 정리) 그 사이 마감이 연장·해제됐으면 건너뛴다.
    public boolean closeIfDue(Long pollId) {
        Boolean closed = transactionTemplate.execute(status -> {
            SchedulePoll poll = schedulePollRepository.findByIdForUpdate(pollId).orElse(null);
            LocalDateTime now = LocalDateTime.now(clock);
            if (poll == null || poll.isDeadlineProcessed() || !poll.isDeadlinePassed(now)) {
                return false;
            }
            close(poll, now);
            poll.increaseRevision();
            return true;
        });
        return Boolean.TRUE.equals(closed);
    }

    // 마감 처리. 요청 중 마감 도래, 개설자 종료, 마감 스케줄러가 모두 이 로직을 쓴다.
    // 조율 행을 잠근 트랜잭션 안에서만 호출한다. revision 증가는 호출하는 쪽이 한다.
    // 이미 처리했으면 아무것도 하지 않는다(여러 번 불려도 한 번만 처리).
    // 참여자 2명 이상이면 최신 결과를 최종 결과로 고정하고 참여자에게 푸시한다(푸시는 커밋 뒤 발송).
    public void close(SchedulePoll poll, LocalDateTime now) {
        if (poll.isClosed() || poll.isDeadlineProcessed()) {
            return;
        }
        poll.close(now);
        if (poll.getParticipantCount() < MIN_PARTICIPANTS_FOR_RESULT) {
            return;
        }
        schedulePollResultService.finalizeLatest(poll);

        List<Long> memberIds = schedulePollSubmissionRepository.findAllInJoinOrder(poll.getId()).stream()
                .map(submission -> submission.getMember().getId())
                .toList();
        // TODO: path는 웹(앱)이 실제로 처리하는 결과 화면 주소를 받아 바꾼다.
        fcmService.prepareTrackedNotification(memberIds, CLOSED_PUSH_TITLE,
                "「" + poll.getTitle() + "」 최종 결과를 확인해 보세요",
                FcmMessageType.SCHEDULE_POLL, poll.getId(), "/schedule-polls/" + poll.getId());
    }

    private record Outcome<T>(T result, boolean closedByDeadline) {

        static <T> Outcome<T> done(T result) {
            return new Outcome<>(result, false);
        }

        static <T> Outcome<T> closed() {
            return new Outcome<>(null, true);
        }
    }
}
