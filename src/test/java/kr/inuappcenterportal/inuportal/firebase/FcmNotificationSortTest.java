package kr.inuappcenterportal.inuportal.firebase;

import com.google.firebase.messaging.FirebaseMessaging;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.res.NotificationResponse;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.NotificationViewService;
import kr.inuappcenterportal.inuportal.domain.image.service.ImageService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.domain.weather.service.WeatherService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FcmNotificationSortTest {

    @MockBean
    WeatherService weatherService;

    @MockBean
    ScheduleService scheduleService;

    @MockBean
    NoticeService noticeService;

    @MockBean
    CafeteriaService cafeteriaService;

    @MockBean
    ImageService imageService;

    @MockBean
    SchoolLoginRepository schoolLoginRepository;

    @MockBean
    RedisService redisService;

    @MockBean
    FirebaseMessaging firebaseMessaging;

    @Autowired
    FcmService fcmService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    FcmMessageRepository fcmMessageRepository;

    @Autowired
    MemberFcmMessageRepository memberFcmMessageRepository;

    @Autowired
    NotificationViewService notificationViewService;

    /**
     * 프록시를 벗긴 원본. 전체 컨텍스트에서는 @Async가 살아 있어 별도 스레드에서 실행되는데,
     * 그 스레드는 테스트 트랜잭션이 아직 커밋하지 않은 데이터를 보지 못한다.
     * 방문 기록 로직 자체를 검증하려고 테스트 트랜잭션 안에서 동기로 호출한다.
     */
    NotificationViewService syncNotificationViewService;

    @BeforeEach
    void unwrapAsyncProxy() {
        syncNotificationViewService = AopTestUtils.getTargetObject(notificationViewService);
    }

    /** 컨트롤러의 GET /api/tokens 1페이지 흐름(조회 후 방문 기록)을 재현한다. */
    private ListResponseDto<NotificationResponse> visitFirstPage(Member member) {
        ListResponseDto<NotificationResponse> response = fcmService.findNotifications(member, 1);
        syncNotificationViewService.recordVisit(member.getId());
        return response;
    }

    @Test
    @DisplayName("알림 조회 시 모든 알림이 최신순(id DESC)으로 정렬되어 반환된다.")
    void notificationLatestSortTest() {
        Member member = memberRepository.save(Member.builder()
                .studentId("202000001")
                .roles(Collections.singletonList("ROLE_USER"))
                .build());

        // 4개의 FCM 메시지 생성
        FcmMessage msg1 = fcmMessageRepository.save(FcmMessage.builder().title("알림 1").body("본문 1").build());
        FcmMessage msg2 = fcmMessageRepository.save(FcmMessage.builder().title("알림 2").body("본문 2").build());
        FcmMessage msg3 = fcmMessageRepository.save(FcmMessage.builder().title("알림 3").body("본문 3").build());
        FcmMessage msg4 = fcmMessageRepository.save(FcmMessage.builder().title("알림 4").body("본문 4").build());

        // memberFcmMessage 생성 (순차적으로 생성)
        MemberFcmMessage m1 = memberFcmMessageRepository.save(MemberFcmMessage.of(msg1.getId(), member.getId(), FcmMessageType.GENERAL));
        MemberFcmMessage m2 = memberFcmMessageRepository.save(MemberFcmMessage.of(msg2.getId(), member.getId(), FcmMessageType.GENERAL));
        MemberFcmMessage m3 = memberFcmMessageRepository.save(MemberFcmMessage.of(msg3.getId(), member.getId(), FcmMessageType.GENERAL));
        MemberFcmMessage m4 = memberFcmMessageRepository.save(MemberFcmMessage.of(msg4.getId(), member.getId(), FcmMessageType.GENERAL));

        // 조회 실행
        ListResponseDto<NotificationResponse> result = fcmService.findNotifications(member, 1);
        List<NotificationResponse> contents = result.getContents();

        assertEquals(4, contents.size());

        // 순수 최신순(id DESC) 정렬 검증: m4 -> m3 -> m2 -> m1
        assertEquals(m4.getId(), contents.get(0).memberFcmMessageId());
        assertEquals(m3.getId(), contents.get(1).memberFcmMessageId());
        assertEquals(m2.getId(), contents.get(2).memberFcmMessageId());
        assertEquals(m1.getId(), contents.get(3).memberFcmMessageId());
    }

    @Test
    @DisplayName("페이지에 상관없이 회원의 모든 안 읽은 알림이 2회 진입 후 3번째 진입 시 일괄 읽음 처리된다.")
    void pageAgnosticAutoReadTest() {
        Member member = memberRepository.save(Member.builder()
                .studentId("202000002")
                .roles(Collections.singletonList("ROLE_USER"))
                .build());

        // 15개의 안 읽은 알림 생성 (1페이지 10개, 2페이지 5개)
        List<MemberFcmMessage> messages = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            FcmMessage msg = fcmMessageRepository.save(FcmMessage.builder().title("알림 " + i).body("본문 " + i).build());
            messages.add(memberFcmMessageRepository.save(
                    MemberFcmMessage.of(msg.getId(), member.getId(), FcmMessageType.GENERAL)
            ));
        }

        // [1번째 방문]: 1페이지(최신 10개)만 조회하고 나감
        ListResponseDto<NotificationResponse> visit1 = visitFirstPage(member);
        assertEquals(10, visit1.getContents().size());
        assertFalse(visit1.getContents().get(0).isRead());
        assertTrue(fcmService.hasUnreadNotification(member));

        // 1페이지에 있던 알림뿐만 아니라 2페이지에 있던 알림(오래된 5개)도 모두 viewCount = 1
        for (MemberFcmMessage msg : messages) {
            MemberFcmMessage loaded = memberFcmMessageRepository.findById(msg.getId()).orElseThrow();
            assertEquals(1, loaded.getViewCount());
            assertFalse(loaded.isRead());
        }

        // [2번째 방문]: 1페이지 조회 -> viewCount = 2, 화면에는 여전히 isRead = false
        ListResponseDto<NotificationResponse> visit2 = visitFirstPage(member);
        assertFalse(visit2.getContents().get(0).isRead());
        assertFalse(fcmService.hasUnreadNotification(member));

        for (MemberFcmMessage msg : messages) {
            MemberFcmMessage loaded = memberFcmMessageRepository.findById(msg.getId()).orElseThrow();
            assertEquals(2, loaded.getViewCount());
        }

        // [3번째 방문]: 2회 조회가 완료되었으므로 15개 전체 알림이 일괄 isRead = true로 전환됨
        // 방문 기록은 응답 이후 비동기로 반영되므로, 이번 응답에는 아직 안 읽음으로 보이고 DB에는 읽음으로 남는다.
        ListResponseDto<NotificationResponse> visit3 = visitFirstPage(member);
        assertFalse(visit3.getContents().get(0).isRead());

        for (MemberFcmMessage msg : messages) {
            MemberFcmMessage loaded = memberFcmMessageRepository.findById(msg.getId()).orElseThrow();
            assertTrue(loaded.isRead());
            assertNotNull(loaded.getReadAt());
        }
    }

    @Test
    @DisplayName("알림 조회 자체는 조회수와 읽음 상태를 바꾸지 않는다(방문 기록은 컨트롤러가 비동기로 따로 남긴다).")
    void findNotificationsHasNoSideEffectTest() {
        Member member = memberRepository.save(Member.builder()
                .studentId("202000004")
                .roles(Collections.singletonList("ROLE_USER"))
                .build());
        FcmMessage msg = fcmMessageRepository.save(FcmMessage.builder().title("알림").body("본문").build());
        MemberFcmMessage saved = memberFcmMessageRepository.save(
                MemberFcmMessage.of(msg.getId(), member.getId(), FcmMessageType.GENERAL));

        for (int i = 0; i < 3; i++) {
            fcmService.findNotifications(member, 1);
        }

        MemberFcmMessage loaded = memberFcmMessageRepository.findById(saved.getId()).orElseThrow();
        assertEquals(0, loaded.getViewCount());
        assertFalse(loaded.isRead());
        assertTrue(fcmService.hasUnreadNotification(member));
    }

    @Test
    @DisplayName("인피니티 스크롤 시(1p -> 2p) 정렬 순서가 흔들리지 않고 순차적으로 깨끗하게 최신순으로 조회된다.")
    void infiniteScrollConsistencyTest() {
        Member member = memberRepository.save(Member.builder()
                .studentId("202000003")
                .roles(Collections.singletonList("ROLE_USER"))
                .build());

        // 안 읽은 알림 15개 생성
        for (int i = 1; i <= 15; i++) {
            FcmMessage msg = fcmMessageRepository.save(FcmMessage.builder().title("알림 " + i).body("본문 " + i).build());
            memberFcmMessageRepository.save(
                    MemberFcmMessage.of(msg.getId(), member.getId(), FcmMessageType.GENERAL)
            );
        }

        // 1페이지 조회 (최신 10개)
        ListResponseDto<NotificationResponse> page1 = fcmService.findNotifications(member, 1);
        assertEquals(10, page1.getContents().size());

        // 2페이지 조회 (그 다음 5개)
        ListResponseDto<NotificationResponse> page2 = fcmService.findNotifications(member, 2);
        assertEquals(5, page2.getContents().size());

        List<Long> page1Ids = page1.getContents().stream().map(NotificationResponse::memberFcmMessageId).toList();
        List<Long> page2Ids = page2.getContents().stream().map(NotificationResponse::memberFcmMessageId).toList();

        // 1페이지와 2페이지 아이템 ID에 중복이 전혀 없어야 함
        for (Long id : page2Ids) {
            assertFalse(page1Ids.contains(id), "2페이지 아이템이 1페이지에 중복 포함되어서는 안 됩니다: id=" + id);
        }

        assertEquals(15, page1Ids.size() + page2Ids.size());
    }
}
