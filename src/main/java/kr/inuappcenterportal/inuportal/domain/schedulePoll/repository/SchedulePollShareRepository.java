package kr.inuappcenterportal.inuportal.domain.schedulePoll.repository;

import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ShareKind;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SchedulePollShareRepository extends JpaRepository<SchedulePollShare, Long> {

    Optional<SchedulePollShare> findByPollIdAndKindAndResultVersionAndChatRoomId(
            Long pollId, ShareKind kind, int resultVersion, Long chatRoomId);

    // 카드 메시지를 보내지 못했을 때 기록을 되돌린다.
    @Transactional
    void deleteByPollIdAndChatMessageId(Long pollId, Long chatMessageId);

    // 마감 1시간 전 알림용. 공유 순서대로
    List<SchedulePollShare> findAllByPollIdOrderByIdAsc(Long pollId);
}
