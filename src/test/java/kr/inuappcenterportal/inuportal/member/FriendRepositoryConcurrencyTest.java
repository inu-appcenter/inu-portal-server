package kr.inuappcenterportal.inuportal.member;

import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 수락(조건부 UPDATE)과 거절·취소(조건부 DELETE)가 서로 다른 트랜잭션에서 동시에 들어올 때
 * 정확히 하나만 성공하는지 확인한다. 진 쪽이 0행을 받든 DB 잠금 예외를 받든 "성공"은 아니다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FriendRepositoryConcurrencyTest {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        friendRepository.deleteAllInBatch();
        memberRepository.deleteAll();
    }

    @RepeatedTest(10)
    @DisplayName("같은 PENDING 요청에 수락과 거절이 동시에 오면 하나만 성공하고 상태가 어긋나지 않는다")
    void acceptAndRejectRace_exactlyOneWins() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long friendId = tx.execute(status -> {
            Member requester = memberRepository.save(
                    Member.builder().studentId("209800001").roles(List.of("ROLE_USER")).build());
            Member receiver = memberRepository.save(
                    Member.builder().studentId("209800002").roles(List.of("ROLE_USER")).build());
            return friendRepository.save(Friend.builder()
                    .requester(requester).receiver(receiver).status(FriendStatus.PENDING).build()).getId();
        });

        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> accept = () -> {
            start.await();
            Integer n = tx.execute(s -> friendRepository.updateStatusIfMatches(
                    friendId, FriendStatus.PENDING, FriendStatus.ACCEPTED, LocalDateTime.now()));
            return n != null && n == 1;
        };
        Callable<Boolean> reject = () -> {
            start.await();
            Integer n = tx.execute(s -> friendRepository.deleteIfStatus(friendId, FriendStatus.PENDING));
            return n != null && n == 1;
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> acceptResult = pool.submit(accept);
            Future<Boolean> rejectResult = pool.submit(reject);
            start.countDown();
            boolean accepted = succeeded(acceptResult);
            boolean rejected = succeeded(rejectResult);

            assertThat(accepted ^ rejected).as("정확히 하나만 성공").isTrue();
            Optional<Friend> row = friendRepository.findById(friendId);
            if (accepted) {
                assertThat(row).isPresent();
                assertThat(row.get().getStatus()).isEqualTo(FriendStatus.ACCEPTED);
            } else {
                assertThat(row).isEmpty();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static boolean succeeded(Future<Boolean> result) throws InterruptedException {
        try {
            return result.get(30, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            return false;
        }
    }
}
