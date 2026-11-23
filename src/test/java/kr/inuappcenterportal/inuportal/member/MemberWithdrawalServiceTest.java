package kr.inuappcenterportal.inuportal.member;

import jakarta.persistence.EntityManager;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.folder.model.Folder;
import kr.inuappcenterportal.inuportal.domain.folder.repository.FolderRepository;
import kr.inuappcenterportal.inuportal.domain.folderPost.model.FolderPost;
import kr.inuappcenterportal.inuportal.domain.folderPost.repository.FolderPostRepository;
import kr.inuappcenterportal.inuportal.domain.keyword.domain.Keyword;
import kr.inuappcenterportal.inuportal.domain.keyword.repository.KeywordRepository;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.enums.RealNameSource;
import kr.inuappcenterportal.inuportal.domain.member.model.Block;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.FriendInviteCode;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendInviteCodeRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberWithdrawalService;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.scrap.model.Scrap;
import kr.inuappcenterportal.inuportal.domain.scrap.repository.ScrapRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 회원탈퇴 (#541) 통합 테스트.
 * 실제 JPA(H2)로 돌려서 새로 추가한 삭제 쿼리와 FK 순서가 맞는지까지 확인한다.
 * 시간표 삭제는 기존 TimeTableService 로직을 그대로 호출하는지만 확인한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(MemberWithdrawalService.class)
class MemberWithdrawalServiceTest {

    @Autowired private MemberWithdrawalService memberWithdrawalService;
    @Autowired private EntityManager em;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private FriendRepository friendRepository;
    @Autowired private BlockRepository blockRepository;
    @Autowired private FriendInviteCodeRepository friendInviteCodeRepository;
    @Autowired private KeywordRepository keywordRepository;
    @Autowired private FolderRepository folderRepository;
    @Autowired private ScrapRepository scrapRepository;
    @Autowired private FolderPostRepository folderPostRepository;
    @Autowired private FcmTokenRepository fcmTokenRepository;
    @Autowired private MemberFcmMessageRepository memberFcmMessageRepository;

    @MockBean private TimeTableService timeTableService;

    @Test
    @DisplayName("탈퇴하면 개인정보는 지워지고, 개인화 데이터는 삭제되고, 게시글은 남는다")
    void withdraw_anonymizesAndDeletesPersonalData() {
        // given
        Member member = persistMember("202600001");
        member.updateNicknameAndFire("탈퇴할회원", 5L);
        member.updateRealName("홍길동", RealNameSource.MANUAL);
        member.updateLocation(37.37, 126.63);
        member.updateNearbyVisibility(true);
        Member other = persistMember("202600002");

        Post post = Post.builder().title("제목").content("내용").category("자유").anonymous(false).member(member).imageCount(0).build();
        em.persist(post);
        Post otherPost = Post.builder().title("남의 글").content("내용").category("자유").anonymous(false).member(other).imageCount(0).build();
        em.persist(otherPost);

        em.persist(Friend.builder().requester(member).receiver(other).status(FriendStatus.ACCEPTED).build());
        em.persist(Friend.builder().requester(other).receiver(member).status(FriendStatus.PENDING).build());
        em.persist(Block.builder().blocker(other).blocked(member).build());
        em.persist(FriendInviteCode.builder().member(member).code("INVITE01").build());
        em.persist(Keyword.builder().memberId(member.getId()).keyword("장학").type(FcmMessageType.SCHOOL_NOTICE).build());

        Folder folder = Folder.builder().name("내 폴더").member(member).build();
        em.persist(folder);
        Scrap scrap = Scrap.builder().member(member).post(otherPost).build();
        em.persist(scrap);
        em.persist(FolderPost.builder().post(otherPost).folder(folder).scrap(scrap).build());

        em.persist(new FcmToken(member.getId(), "device-token-1", "ios"));
        em.persist(MemberFcmMessage.of(1L, member.getId(), FcmMessageType.GENERAL));
        em.flush();
        em.clear();

        // when
        memberWithdrawalService.withdraw(member.getId());
        em.flush();
        em.clear();

        // then: 회원 레코드는 남고 개인정보는 지워짐
        Member withdrawn = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(withdrawn.isWithdrawn()).isTrue();
        assertThat(withdrawn.getDeletedAt()).isNotNull();
        assertThat(withdrawn.getNickname()).isEqualTo(Member.WITHDRAWN_NICKNAME);
        assertThat(withdrawn.getStudentId()).isNotEqualTo("202600001");
        assertThat(withdrawn.getRealName()).isNull();
        assertThat(withdrawn.getLatitude()).isNull();
        assertThat(withdrawn.getLongitude()).isNull();
        assertThat(withdrawn.getNearbyVisibility()).isFalse();
        assertThat(withdrawn.getFireId()).isEqualTo(1L);
        assertThat(withdrawn.getRoles()).isEmpty();

        // 게시글은 남고 작성자는 '탈퇴한 사용자'로 보임
        Post keptPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(keptPost.getMember().getNickname()).isEqualTo(Member.WITHDRAWN_NICKNAME);

        // 친구·차단·초대코드·키워드·폴더·스크랩·알림함 삭제 (상대방 쪽 관계도 사라짐)
        assertThat(friendRepository.findAll()).isEmpty();
        assertThat(blockRepository.findAll()).isEmpty();
        assertThat(friendInviteCodeRepository.findAll()).isEmpty();
        assertThat(keywordRepository.findAllByMemberId(member.getId())).isEmpty();
        assertThat(folderPostRepository.findAll()).isEmpty();
        assertThat(folderRepository.findAll()).isEmpty();
        assertThat(scrapRepository.findAll()).isEmpty();
        assertThat(memberFcmMessageRepository.findAll()).isEmpty();

        // 푸시 토큰도 삭제
        assertThat(fcmTokenRepository.findByToken("device-token-1")).isEmpty();

        // 다른 회원의 데이터는 그대로
        assertThat(postRepository.findById(otherPost.getId())).isPresent();
        assertThat(memberRepository.findById(other.getId()).orElseThrow().isWithdrawn()).isFalse();
    }

    @Test
    @DisplayName("시간표는 기존 시간표 삭제 로직으로 하나씩 지운다")
    void withdraw_deletesTimeTablesThroughExistingService() {
        // given
        Member member = persistMember("202600003");
        Semester semester = Semester.create(2026, SemesterTerm.SECOND, SemesterStatus.OPEN,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20));
        em.persist(semester);
        TimeTable first = TimeTable.create("시간표1", true, member, semester);
        TimeTable second = TimeTable.create("시간표2", false, member, semester);
        em.persist(first);
        em.persist(second);
        em.flush();

        // when
        memberWithdrawalService.withdraw(member.getId());

        // then
        verify(timeTableService).deleteTimeTable(member.getId(), first.getId());
        verify(timeTableService).deleteTimeTable(member.getId(), second.getId());
    }

    @Test
    @DisplayName("이미 탈퇴한 회원은 다시 처리하지 않는다")
    void withdraw_isIdempotent() {
        // given
        Member member = persistMember("202600004");
        em.flush();
        memberWithdrawalService.withdraw(member.getId());
        em.flush();
        em.clear();
        Member afterFirst = memberRepository.findById(member.getId()).orElseThrow();

        // when
        memberWithdrawalService.withdraw(member.getId());
        em.flush();
        em.clear();

        // then: 탈퇴 일시가 바뀌지 않음
        Member afterSecond = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(afterSecond.getDeletedAt()).isEqualTo(afterFirst.getDeletedAt());
    }

    @Test
    @DisplayName("탈퇴 후 같은 학번은 비어 있어서 다시 로그인하면 새 계정이 만들어진다")
    void withdraw_freesStudentIdForRejoin() {
        // given
        Member member = persistMember("202600005");
        em.flush();

        // when
        memberWithdrawalService.withdraw(member.getId());
        em.flush();
        em.clear();

        // then: schoolLogin()은 findByStudentId가 비어 있으면 새 회원을 만든다
        assertThat(memberRepository.findByStudentId("202600005")).isEmpty();
        verify(timeTableService, never()).deleteTimeTable(anyLong(), anyLong());
    }

    @Test
    @DisplayName("탈퇴 회원은 닉네임 검색과 알림 대상 조회에 잡히지 않는다")
    void withdrawnMembersAreExcludedFromLookups() {
        // given: 탈퇴 회원 둘(닉네임이 모두 '탈퇴한 사용자')과 일반 회원 하나
        Member withdrawn1 = persistMember("202600006");
        Member withdrawn2 = persistMember("202600007");
        Member active = persistMember("202600008");
        em.flush();
        memberWithdrawalService.withdraw(withdrawn1.getId());
        memberWithdrawalService.withdraw(withdrawn2.getId());
        em.flush();
        em.clear();

        // then
        assertThat(memberRepository.findByNicknameAndDeletedAtIsNull(Member.WITHDRAWN_NICKNAME)).isEmpty();
        assertThat(memberRepository.findAllIds()).containsExactly(active.getId());
        assertThat(memberRepository.findAllActive()).extracting(Member::getId).containsExactly(active.getId());
    }

    private Member persistMember(String studentId) {
        Member member = Member.builder().studentId(studentId).roles(List.of("ROLE_USER")).build();
        em.persist(member);
        return member;
    }
}
