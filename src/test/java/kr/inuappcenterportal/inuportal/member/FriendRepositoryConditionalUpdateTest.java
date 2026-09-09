package kr.inuappcenterportal.inuportal.member;

import jakarta.persistence.EntityManager;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FriendRepositoryConditionalUpdateTest {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("PENDING일 때만 수락되고, 두 번째 수락은 0행이다")
    void updateStatusIfMatches_onlyFirstWins() {
        Member requester = Member.builder().studentId("209900001").roles(List.of("ROLE_USER")).build();
        Member receiver = Member.builder().studentId("209900002").roles(List.of("ROLE_USER")).build();
        em.persist(requester);
        em.persist(receiver);
        Friend friend = friendRepository.save(
                Friend.builder().requester(requester).receiver(receiver).status(FriendStatus.PENDING).build());
        em.flush();

        int first = friendRepository.updateStatusIfMatches(
                friend.getId(), FriendStatus.PENDING, FriendStatus.ACCEPTED, LocalDateTime.now());
        int second = friendRepository.updateStatusIfMatches(
                friend.getId(), FriendStatus.PENDING, FriendStatus.ACCEPTED, LocalDateTime.now());

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        em.clear();
        assertThat(friendRepository.findById(friend.getId()).orElseThrow().getStatus())
                .isEqualTo(FriendStatus.ACCEPTED);
    }
}
