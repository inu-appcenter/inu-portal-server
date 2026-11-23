package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.agent.repository.AgentReminderRepository;
import kr.inuappcenterportal.inuportal.domain.agent.repository.CampusWatchJobRepository;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.repository.DailyBriefSettingRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.LiveActivityInstanceRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.folder.repository.FolderRepository;
import kr.inuappcenterportal.inuportal.domain.folderPost.repository.FolderPostRepository;
import kr.inuappcenterportal.inuportal.domain.keyword.repository.KeywordRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendInviteCodeRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.GradeRecordRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.repository.MockEnrollmentRepository;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.repository.MockWatchlistRepository;
import kr.inuappcenterportal.inuportal.domain.scrap.repository.ScrapRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableService;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원탈퇴 (#541)
 *
 * <p>Member 레코드는 지우지 않는다(Soft Delete). 게시글·댓글·좋아요가 member를 FK로 물고 있어서,
 * 레코드를 남겨 관계를 지키고 개인을 알아볼 수 있는 정보만 지운다.
 * 개인화 데이터(시간표, 성적, 알림 설정 등)는 바로 삭제한다.
 *
 * <p>정책상 아직 정하지 않은 데이터(채팅, 시간 맞추기, 청원·건의·신고·분실물·대여, 실명 동의 이력,
 * 서비스 이용 로그)는 건드리지 않는다. 닉네임이 '탈퇴한 사용자'로 바뀌므로 화면에는 그렇게 보인다.
 *
 * <p>모든 처리는 한 트랜잭션이라, 중간에 실패하면 전부 되돌아가고 회원은 탈퇴 전 상태로 남는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {
    private final MemberRepository memberRepository;
    private final TimeTableRepository timeTableRepository;
    private final TimeTableService timeTableService;
    private final GradeRecordRepository gradeRecordRepository;
    private final DailyBriefSettingRepository dailyBriefSettingRepository;
    private final KeywordRepository keywordRepository;
    private final FolderPostRepository folderPostRepository;
    private final FolderRepository folderRepository;
    private final ScrapRepository scrapRepository;
    private final AgentReminderRepository agentReminderRepository;
    private final CampusWatchJobRepository campusWatchJobRepository;
    private final MockEnrollmentRepository mockEnrollmentRepository;
    private final MockWatchlistRepository mockWatchlistRepository;
    private final FriendRepository friendRepository;
    private final BlockRepository blockRepository;
    private final FriendInviteCodeRepository friendInviteCodeRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final LiveActivityInstanceRepository liveActivityInstanceRepository;
    private final MemberFcmMessageRepository memberFcmMessageRepository;

    @Transactional
    public void withdraw(Long memberId) {
        // 행을 잠그고 읽어서, 같은 회원의 탈퇴 요청이 동시에 와도 한 번만 처리한다.
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        if (member.isWithdrawn()) {
            log.info("[회원탈퇴] 이미 탈퇴한 회원이라 건너뜀 memberId={}", memberId);
            return;
        }

        deletePersonalData(memberId);
        detachDevices(memberId);
        deleteRelationships(memberId);

        member.withdraw(LocalDateTime.now());
        log.info("[회원탈퇴] 완료 memberId={}", memberId);
    }

    // 개인화 데이터: 시간표, 성적, 알림 설정·키워드, 스크랩·폴더, 에이전트 알림·감시, 모의수강신청
    private void deletePersonalData(Long memberId) {
        // 시간표는 항목·직접 추가한 일정·평가까지 지우는 기존 삭제 로직을 그대로 쓴다.
        List<TimeTable> timeTables = timeTableRepository.findAllByMemberId(memberId);
        for (TimeTable timeTable : timeTables) {
            timeTableService.deleteTimeTable(memberId, timeTable.getId());
        }
        gradeRecordRepository.deleteAllByMemberIdForWithdrawal(memberId);
        dailyBriefSettingRepository.deleteAllByMemberIdForWithdrawal(memberId);
        keywordRepository.deleteAllByMemberIdForWithdrawal(memberId);
        // 폴더 안 게시글(folder_post)이 폴더·스크랩을 FK로 물고 있어서 먼저 지운다.
        folderPostRepository.deleteAllByMemberIdForWithdrawal(memberId);
        folderRepository.deleteAllByMemberIdForWithdrawal(memberId);
        scrapRepository.deleteAllByMemberIdForWithdrawal(memberId);
        agentReminderRepository.deleteAllByMemberIdForWithdrawal(memberId);
        campusWatchJobRepository.deleteAllByMemberIdForWithdrawal(memberId);
        mockEnrollmentRepository.deleteAllByMemberIdForWithdrawal(memberId);
        mockWatchlistRepository.deleteAllByMemberIdForWithdrawal(memberId);
    }

    // 기기 연결: 푸시 토큰, 진행 중인 Live Activity, 알림함을 지운다.
    // 토큰을 지우면 그 기기로는 개인 알림도 일반 공지도 가지 않는다. 앱이 다시 토큰을 등록하면 비로그인 기기로 새로 저장된다.
    private void detachDevices(Long memberId) {
        fcmTokenRepository.deleteAllByMemberIdForWithdrawal(memberId);
        liveActivityInstanceRepository.deleteAllByMemberIdForWithdrawal(memberId);
        memberFcmMessageRepository.deleteAllByMemberIdForWithdrawal(memberId);
    }

    // 친구·차단·초대코드: 상대방 목록에서도 사라진다.
    private void deleteRelationships(Long memberId) {
        friendRepository.deleteAllByMemberIdForWithdrawal(memberId);
        blockRepository.deleteAllByMemberIdForWithdrawal(memberId);
        friendInviteCodeRepository.deleteAllByMemberIdForWithdrawal(memberId);
    }
}
