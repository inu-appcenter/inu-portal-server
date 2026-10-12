package kr.inuappcenterportal.inuportal.domain.schedulePoll.repository;

import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ShareKind;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchedulePollShareRepository extends JpaRepository<SchedulePollShare, Long> {

    Optional<SchedulePollShare> findByPollIdAndKindAndResultVersionAndChatRoomId(
            Long pollId, ShareKind kind, int resultVersion, Long chatRoomId);

    // 마감 1시간 전 알림용. 공유 순서대로
    List<SchedulePollShare> findAllByPollIdOrderByIdAsc(Long pollId);
}
