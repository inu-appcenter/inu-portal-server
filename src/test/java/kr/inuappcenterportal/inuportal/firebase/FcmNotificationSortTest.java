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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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

    @Test
    @DisplayName("알림 조회 시 안 읽은 알림이 최신순으로 상단에 배치되고, 그 후 읽은 알림이 최신순으로 배치된다.")
    void notificationSortOrderTest() {
        Member member = memberRepository.save(Member.builder()
                .studentId("202000001")
                .roles(Collections.singletonList("ROLE_USER"))
                .build());

        // 4개의 FCM 메시지 생성
        FcmMessage msg1 = fcmMessageRepository.save(FcmMessage.builder().title("알림 1").body("본문 1").build());
        FcmMessage msg2 = fcmMessageRepository.save(FcmMessage.builder().title("알림 2").body("본문 2").build());
        FcmMessage msg3 = fcmMessageRepository.save(FcmMessage.builder().title("알림 3").body("본문 3").build());
        FcmMessage msg4 = fcmMessageRepository.save(FcmMessage.builder().title("알림 4").body("본문 4").build());

        // memberFcmMessage 생성
        // msg1: 읽음 상태
        MemberFcmMessage m1 = MemberFcmMessage.of(msg1.getId(), member.getId(), FcmMessageType.GENERAL);
        m1.markAsRead();
        m1 = memberFcmMessageRepository.save(m1);

        // msg2: 안 읽은 상태 (오래된 안 읽은 알림)
        MemberFcmMessage m2 = MemberFcmMessage.of(msg2.getId(), member.getId(), FcmMessageType.GENERAL);
        m2 = memberFcmMessageRepository.save(m2);

        // msg3: 읽음 상태 (최신 읽은 알림)
        MemberFcmMessage m3 = MemberFcmMessage.of(msg3.getId(), member.getId(), FcmMessageType.GENERAL);
        m3.markAsRead();
        m3 = memberFcmMessageRepository.save(m3);

        // msg4: 안 읽은 상태 (최신 안 읽은 알림)
        MemberFcmMessage m4 = MemberFcmMessage.of(msg4.getId(), member.getId(), FcmMessageType.GENERAL);
        m4 = memberFcmMessageRepository.save(m4);

        // 조회 실행
        ListResponseDto<NotificationResponse> result = fcmService.findNotifications(member, 1);
        List<NotificationResponse> contents = result.getContents();

        assertEquals(4, contents.size());

        // 순서 검증:
        // 1등: m4 (안 읽음, id 더 큼)
        // 2등: m2 (안 읽음, id 더 작음)
        // 3등: m3 (읽음, id 더 큼)
        // 4등: m1 (읽음, id 더 작음)
        assertEquals(m4.getId(), contents.get(0).memberFcmMessageId());
        assertFalse(contents.get(0).isRead());

        assertEquals(m2.getId(), contents.get(1).memberFcmMessageId());
        assertFalse(contents.get(1).isRead());

        assertEquals(m3.getId(), contents.get(2).memberFcmMessageId());
        assertTrue(contents.get(2).isRead());

        assertEquals(m1.getId(), contents.get(3).memberFcmMessageId());
        assertTrue(contents.get(3).isRead());
    }

    @Test
    @DisplayName("알림을 2회 조회하면 자동으로 읽음 처리된다.")
    void viewCountIncrementAndAutoReadTest() {
        Member member = memberRepository.save(Member.builder()
                .studentId("202000002")
                .roles(Collections.singletonList("ROLE_USER"))
                .build());

        FcmMessage msg = fcmMessageRepository.save(FcmMessage.builder().title("알림").body("본문").build());
        MemberFcmMessage memberMsg = memberFcmMessageRepository.save(
                MemberFcmMessage.of(msg.getId(), member.getId(), FcmMessageType.GENERAL)
        );

        // 첫 번째 조회 -> viewCount = 1, isRead = false
        ListResponseDto<NotificationResponse> firstFetch = fcmService.findNotifications(member, 1);
        assertFalse(firstFetch.getContents().get(0).isRead());

        MemberFcmMessage afterFirst = memberFcmMessageRepository.findById(memberMsg.getId()).orElseThrow();
        assertEquals(1, afterFirst.getViewCount());
        assertFalse(afterFirst.isRead());

        // 두 번째 조회 -> viewCount = 2, isRead = true
        ListResponseDto<NotificationResponse> secondFetch = fcmService.findNotifications(member, 1);
        assertTrue(secondFetch.getContents().get(0).isRead());

        MemberFcmMessage afterSecond = memberFcmMessageRepository.findById(memberMsg.getId()).orElseThrow();
        assertEquals(2, afterSecond.getViewCount());
        assertTrue(afterSecond.isRead());
        assertNotNull(afterSecond.getReadAt());
    }
}
